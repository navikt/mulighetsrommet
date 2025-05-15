/**
 * Fixes Tailwind CSS content for use in Shadow DOM by properly escaping arbitrary values.
 * This solves issues with complex selectors containing brackets, percentages and other special characters
 * that need escaping when inserted via innerHTML.
 * 
 * @param css The CSS string to fix
 * @returns Fixed CSS with properly escaped selectors
 */
export function fixTailwindCssForShadowDom(css: string): string {
  // Fix arbitrary values in various Tailwind utilities
  return css.replace(
    // Match Tailwind classes with arbitrary values like:
    // .grid-cols-[0_40%_1fr_2%]
    // .gap-[5px]
    // .text-[#000000]
    // .border-[rgba(7,26,54,0.21)]
    /\.([\w-]+)-\[(.*?)\]/g,
    (match) => {
      // Escape square brackets, percentage signs, and other special characters
      return match
        .replace(/\[/g, '\\[')
        .replace(/\]/g, '\\]')
        .replace(/%/g, '\\%')
        .replace(/\(/g, '\\(')
        .replace(/\)/g, '\\)');
    }
  );
}