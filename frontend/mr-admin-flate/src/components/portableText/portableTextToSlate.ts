import { BaseElement, Descendant } from "slate";
import type { PortableTextBlock } from "@portabletext/types";

export const portableTextToSlate = (pt: PortableTextBlock[]): Descendant[] => {
  const descendants: BaseElement[] = [];

  for (const block of pt) {
    if (block._type !== "block") throw Error("unsupported portabletext block");

    if (block.listItem === "bullet") {
      const prevDescendant = descendants.at(-1);
      if (prevDescendant?.type === "bulleted-list") {
        prevDescendant.children.push({
          type: "list-item",
          children: children(block),
        });
      } else {
        descendants.push({
          type: "bulleted-list",
          children: [
            {
              type: "list-item",
              children: children(block),
            },
          ],
        });
      }
    } else {
      descendants.push({
        type: block.style === "h1" ? "heading-one" : "paragraph",
        children: children(block),
      });
    }
  }

  return descendants;
};

const children = (block: PortableTextBlock): Descendant[] => {
  const children: Descendant[] = [];
  for (const child of block.children) {
    const markDef = block.markDefs?.find((m) => child.marks?.find((cm: string) => cm === m._key));
    if (markDef) {
      // Found a link
      children.push({
        type: "link",
        url: markDef.href as string,
        children: [
          {
            text: child.text,
            ...findMarks(child.marks),
          },
        ],
      });
    } else {
      children.push({
        text: child.text,
        ...findMarks(child.marks),
      });
    }
  }

  return children;
};

const findMarks = (marks?: string[]) => {
  if (!marks) {
    return undefined;
  }
  return {
    ...(Boolean(marks.find((m) => m === "strong")) && { bold: true }),
    ...(Boolean(marks.find((m) => m === "em")) && { italic: true }),
  };
};
