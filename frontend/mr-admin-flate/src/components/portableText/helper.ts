import { PortableTextObject, PortableTextBlock } from "@portabletext/editor";

export enum SupportedDecorator {
  STRONG = "strong",
  EM = "em",
  UNDERLINE = "underline",
}

export enum SupportedStyle {
  NORMAL = "normal",
}

export enum SupportedAnnotation {
  LINK = "link",
}

export enum SupportedList {
  BULLET = "bullet",
  NUMBER = "number",
}

function getOrAddKey(obj: { _key: string | null }): string {
  return obj._key ?? crypto.randomUUID().slice(0, 8);
}

// PortableText editor requires _key to not be null in certain blocks
// Only attemt to fix block if null-key is detected
export function convertSlateToPortableText(slateData: PortableTextBlock[]): PortableTextBlock[] {
  if (!slateData || !slateData.length) {
    return [];
  }
  if (!slateData.some((block) => block._key === null)) {
    // not from slate
    return slateData;
  }
  return slateData.map((block) => {
    const newBlock: PortableTextBlock = { ...block };
    if (block._type === "block") {
      newBlock["style"] = "normal";
      // required to display anything
      newBlock._key = getOrAddKey(newBlock);
      if ("listItem" in newBlock) {
        // indent existing lists
        newBlock["level"] = 1;
      }
      // Fix existing links
      if ("markDefs" in newBlock && Array.isArray(newBlock.markDefs)) {
        newBlock.markDefs = newBlock.markDefs ?? [];
        const linkMarkDefIndex = ((newBlock.markDefs || []) as PortableTextObject[]).findIndex(
          (obj) => obj._type === SupportedAnnotation.LINK,
        );
        if (linkMarkDefIndex > -1) {
          const linkMarkDef = (newBlock.markDefs as PortableTextObject[])[linkMarkDefIndex];
          const newKey = getOrAddKey({ _key: null });
          newBlock.children = (newBlock.children as PortableTextObject[]).map((child) => {
            if ("marks" in child) {
              child.marks = (child.marks as string[]).map((mark) => {
                if (mark === linkMarkDef._key) {
                  return newKey;
                }
                return mark;
              });
            }
            child._key = getOrAddKey(child);
            return child;
          });
          (newBlock.markDefs as PortableTextObject[])[linkMarkDefIndex] = {
            ...linkMarkDef,
            _key: newKey,
          };
        }
      }
    }
    return newBlock;
  });
}
