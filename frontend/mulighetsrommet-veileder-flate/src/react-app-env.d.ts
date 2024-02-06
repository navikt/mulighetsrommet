/// <reference types="vite" />

/**
 * Blir populert med css-styling via pluginen `vite-plugin-shadow-style` [0] og gjør det lettere å mounte CSS
 * under en shadow root runtime.
 *
 * Merk at denne enn så lenge ikke er tilgjgenglig i dev-modus [1].
 *
 * [0] https://github.com/hood/vite-plugin-shadow-style
 * [1] https://github.com/hood/vite-plugin-shadow-style/issues/5
 */
declare const SHADOW_STYLE: string;
