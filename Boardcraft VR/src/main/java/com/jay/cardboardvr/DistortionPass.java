package com.jay.cardboardvr;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.backend.opengl.GlTexture;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.nio.ByteBuffer;

/**
 * Cardboard lens pre-distortion on the finished SBS frame (UNTESTED).
 *
 * Copies the main render target into our own texture, then redraws it into the main target through a shader.
 * Per output pixel: screen mm -> tan angle around that eye's lens centre (t = (mm - lensCentre) / screenToLens),
 * p' = t * (1 + k1 r^2 + k2 r^4), then sample that eye's half of the copy at p' (black outside the image).
 * All raw GL state we touch is read first and put back after, so Minecraft's cached GL state stays valid.
 */
public final class DistortionPass {
    private static final String VS = """
            #version 330 core
            void main() {
                vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
            }
            """;

    private static final String FS = """
            #version 330 core
            uniform sampler2D uSrc;
            uniform vec2 uFb;          // frame size in pixels
            uniform vec2 uScreenMm;    // physical screen size in mm
            uniform vec2 uLensL;       // left lens centre, mm from the screen's bottom-left
            uniform vec2 uLensR;       // right lens centre
            uniform float uS2L;        // screen-to-lens distance, mm
            uniform vec2 uK;           // k1, k2
            uniform vec2 uSrcTan;      // half extent of the rendered eye image in tan-angle (x, y)
            out vec4 fragColor;
            void main() {
                vec2 px = gl_FragCoord.xy;
                bool right = px.x >= uFb.x * 0.5;
                vec2 mm = px / uFb * uScreenMm;
                vec2 t = (mm - (right ? uLensR : uLensL)) / uS2L;
                float r2 = dot(t, t);
                vec2 p = t * (1.0 + uK.x * r2 + uK.y * r2 * r2);
                vec2 uv = p / (2.0 * uSrcTan) + 0.5;
                if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
                    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
                    return;
                }
                float sx = (uv.x + (right ? 1.0 : 0.0)) * 0.5;
                fragColor = vec4(texture(uSrc, vec2(sx, uv.y)).rgb, 1.0);
            }
            """;

    private static boolean failed, loggedOnce;
    private static int prog, vao, fbo, copyTex, sampler, copyW, copyH;
    private static int uSrc, uFb, uScreenMm, uLensL, uLensR, uS2L, uK, uSrcTan;

    private DistortionPass() {}

    public static void run() {
        if (failed) return;
        VrConfig c = VrConfig.I;
        if (!c.distortion || !c.hasViewer || c.dist.length == 0 || c.screenToLensMm <= 0f || !StereoBridge.stereoOn()) return;

        int pProg = 0, pVao = 0, pActive = 0, pDraw = 0, pRead = 0, pTex0 = 0, pSamp0 = 0;
        int[] vp = new int[4];
        boolean sc = false, bl = false, dt = false, cf = false, st = false, saved = false;
        try {
            Minecraft mc = Minecraft.getInstance();
            RenderTarget main = mc.gameRenderer.mainRenderTarget();
            if (main == null || !(main.getColorTexture() instanceof GlTexture gl)) return;
            int tex = gl.glId(), w = main.width, h = main.height;
            if (tex == 0 || w < 16 || h < 16) return;

            if (prog == 0 && !init()) { failed = true; return; }

            // ---- remember GL state ----
            pProg = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            pVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            pActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            pDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            pRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            pTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            pSamp0 = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
            sc = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            bl = GL11.glIsEnabled(GL11.GL_BLEND);
            dt = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            cf = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            st = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
            saved = true;

            // ---- copy the finished frame ----
            if (copyW != w || copyH != h) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, copyTex);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                copyW = w;
                copyH = h;
            }
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, tex, 0);
            if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                if (!loggedOnce) { loggedOnce = true; CardboardVrClient.LOG.warn("Distortion: framebuffer incomplete, skipping"); }
                return;
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, copyTex);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

            // ---- geometry: pixels -> millimetres -> tan angles ----
            float aspect = (float) w / h;
            float wMm, hMm;
            if (c.screenAuto && c.devLongMm > 0f && c.devShortMm > 0f) {   // landscape: long side is the width
                wMm = c.devLongMm;
                hMm = c.devShortMm;
            } else {
                hMm = (c.screenIn * 25.4f) / (float) Math.sqrt(1.0 + aspect * aspect);
                wMm = hMm * aspect;
            }
            float yc = hMm / 2f;
            if (c.trayToLensMm > 0f) {
                if (c.vAlign == 0) yc = c.trayToLensMm;
                else if (c.vAlign == 2) yc = hMm - c.trayToLensMm;
            }
            if (c.vFlip) yc = hMm - yc;
            double vfov = 70.0;
            try { vfov = mc.options.fov().get(); } catch (Throwable ignored) { }
            float tanY = (float) Math.tan(Math.toRadians(vfov) / 2.0);
            float tanX = tanY * (w * 0.5f) / h;
            float k1 = c.dist[0] * c.distStrength, k2 = (c.dist.length > 1 ? c.dist[1] : 0f) * c.distStrength;

            // ---- draw ----
            GL11.glViewport(0, 0, w, h);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glColorMask(true, true, true, true);
            GL20.glUseProgram(prog);
            GL33.glBindSampler(0, sampler);
            GL20.glUniform1i(uSrc, 0);
            GL20.glUniform2f(uFb, w, h);
            GL20.glUniform2f(uScreenMm, wMm, hMm);
            GL20.glUniform2f(uLensL, wMm / 2f - c.lensMm / 2f, yc);
            GL20.glUniform2f(uLensR, wMm / 2f + c.lensMm / 2f, yc);
            GL20.glUniform1f(uS2L, c.screenToLensMm);
            GL20.glUniform2f(uK, k1, k2);
            GL20.glUniform2f(uSrcTan, tanX, tanY);
            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        } catch (Throwable t) {
            failed = true;
            CardboardVrClient.LOG.warn("Lens distortion failed and was switched off", t);
        } finally {
            if (saved) {
                try {
                    GL30.glBindVertexArray(pVao);
                    GL20.glUseProgram(pProg);
                    GL33.glBindSampler(0, pSamp0);
                    GL13.glActiveTexture(GL13.GL_TEXTURE0);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, pTex0);
                    GL13.glActiveTexture(pActive);
                    GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, pDraw);
                    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, pRead);
                    GL11.glViewport(vp[0], vp[1], vp[2], vp[3]);
                    setEnabled(GL11.GL_SCISSOR_TEST, sc);
                    setEnabled(GL11.GL_BLEND, bl);
                    setEnabled(GL11.GL_DEPTH_TEST, dt);
                    setEnabled(GL11.GL_CULL_FACE, cf);
                    setEnabled(GL11.GL_STENCIL_TEST, st);
                } catch (Throwable t) {
                    failed = true;
                }
            }
        }
    }

    private static void setEnabled(int cap, boolean on) {
        if (on) GL11.glEnable(cap); else GL11.glDisable(cap);
    }

    private static boolean init() {
        int vs = compile(GL20.GL_VERTEX_SHADER, VS);
        int fs = compile(GL20.GL_FRAGMENT_SHADER, FS);
        if (vs == 0 || fs == 0) return false;
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, vs);
        GL20.glAttachShader(p, fs);
        GL20.glLinkProgram(p);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == 0) {
            CardboardVrClient.LOG.warn("Distortion shader link failed: {}", GL20.glGetProgramInfoLog(p));
            GL20.glDeleteProgram(p);
            return false;
        }
        uSrc = GL20.glGetUniformLocation(p, "uSrc");
        uFb = GL20.glGetUniformLocation(p, "uFb");
        uScreenMm = GL20.glGetUniformLocation(p, "uScreenMm");
        uLensL = GL20.glGetUniformLocation(p, "uLensL");
        uLensR = GL20.glGetUniformLocation(p, "uLensR");
        uS2L = GL20.glGetUniformLocation(p, "uS2L");
        uK = GL20.glGetUniformLocation(p, "uK");
        uSrcTan = GL20.glGetUniformLocation(p, "uSrcTan");
        vao = GL30.glGenVertexArrays();
        fbo = GL30.glGenFramebuffers();
        copyTex = GL11.glGenTextures();
        sampler = GL33.glGenSamplers();
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_WRAP_S, GL12ClampToEdge);
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_WRAP_T, GL12ClampToEdge);
        prog = p;
        CardboardVrClient.LOG.info("Lens distortion shader ready");
        return true;
    }

    private static final int GL12ClampToEdge = 0x812F;

    private static int compile(int type, String src) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == 0) {
            CardboardVrClient.LOG.warn("Distortion shader compile failed: {}", GL20.glGetShaderInfoLog(s));
            GL20.glDeleteShader(s);
            return 0;
        }
        return s;
    }
}
