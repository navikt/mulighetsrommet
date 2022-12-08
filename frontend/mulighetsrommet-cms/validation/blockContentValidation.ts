export function blockContentValidation(
  blocks: { children: { text: string }[] }[] | any,
  maxLength: number,
  errorMessage: string
) {
  if (!blocks) return true;

  const sum: number = blocks
    ?.flatMap((block) => block.children)
    ?.reduce((acc, next) => {
      acc += next.text.length;
      return acc;
    }, 0);
  return sum <= maxLength ? true : errorMessage;
}
