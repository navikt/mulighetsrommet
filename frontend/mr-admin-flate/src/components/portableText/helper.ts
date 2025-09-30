import { PortableTextObject } from "@portabletext/editor";
import { Faneinnhold, PortableTextTypedObject } from "@tiltaksadministrasjon/api-client";

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

function getOrAddKey(obj: { _key?: string | null }): string {
  return obj._key ?? crypto.randomUUID().slice(0, 8);
}

// PortableText editor requires _key to not be null in certain blocks
// Only attemt to fix block if null-key is detected
export function convertSlateToPortableText(
  slateData: PortableTextTypedObject[] | undefined | null,
): PortableTextTypedObject[] | undefined {
  if (!slateData) {
    return undefined;
  }
  if (!slateData.length) {
    return [];
  }
  return slateData.map((block) => {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
    if (block._key !== null) {
      // not from slate
      return block;
    }

    const newBlock: PortableTextTypedObject = { ...block };
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
        newBlock.markDefs = newBlock["markDefs"] ?? [];
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

export function slateFaneinnholdToPortableText(
  faneinnhold: Faneinnhold | null | undefined,
): Partial<Faneinnhold> | null {
  if (!faneinnhold) {
    return null;
  }

  return {
    ...faneinnhold,
    forHvem: convertSlateToPortableText(faneinnhold.forHvem),
    detaljerOgInnhold: convertSlateToPortableText(faneinnhold.detaljerOgInnhold),
    pameldingOgVarighet: convertSlateToPortableText(faneinnhold.pameldingOgVarighet),
    kontaktinfo: convertSlateToPortableText(faneinnhold.kontaktinfo),
    oppskrift: convertSlateToPortableText(faneinnhold.oppskrift),
  };
}
