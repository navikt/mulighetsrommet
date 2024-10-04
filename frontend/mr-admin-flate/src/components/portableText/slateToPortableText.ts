import { BaseText, BaseElement, Element, Node, Descendant } from "slate";
import type {
  PortableTextBlock,
  PortableTextSpan,
  PortableTextMarkDefinition,
} from "@portabletext/types";

// BaseElement doesn't expose the type property for some reason
// https://github.com/ianstormtaylor/slate/issues/4915
declare module "slate" {
  export interface BaseElement {
    type: string;
    markDefs?: PortableTextMarkDefinition[];
    url?: string;
  }
  export interface BaseText {
    italic?: boolean;
    bold?: boolean;
  }
}

export const slateToPortableText = (nodes: Descendant[]): PortableTextBlock[] => {
  const portableBlocks = [];
  for (const node of nodes) {
    if (!Element.isElement(node)) throw Error("Unsupported slate node");

    // Only supported blocks as of now, is <none>, paragraph, bulleted, and heading
    if (node.type === "bulleted-list") {
      portableBlocks.push(...bulletedListBlocks(node));
    } else if (node.type === "heading-one") {
      portableBlocks.push({
        ...toPortableTextBlock(node),
        style: "h1",
      });
      // Sometimes when deleting all elements, an empty 'list-item' remains. We convert this to an empty paragraph
    } else if (node.type === "list-item") {
      portableBlocks.push(toPortableTextBlock(node));
    } else if (node.type && node.type !== "paragraph") {
      throw Error(`Unsupported block type: ${node.type}`);
    } else {
      portableBlocks.push(toPortableTextBlock(node));
    }
  }
  return portableBlocks;
};

const toPortableTextSpan = (span: BaseText): PortableTextSpan => {
  return {
    _type: "span",
    text: Node.string(span),
    marks: findMark(span),
  };
};

const linkToPortableTextSpans = (
  link: BaseElement,
): [PortableTextSpan[], PortableTextMarkDefinition] => {
  if (!link.url) throw Error("link does not have url");

  const markDef: PortableTextMarkDefinition = {
    _type: "link",
    _key: link.url,
    href: link.url,
  };

  const spans = link.children
    .map((s) => toPortableTextSpan(s as BaseText))
    .map((s) => ({
      ...s,
      marks: [...(s?.marks || []), link.url],
    })) as PortableTextSpan[];

  return [spans, markDef];
};

const toPortableTextBlock = (node: Descendant): PortableTextBlock => {
  if (!Element.isElement(node)) throw Error(`Unsupported slate node: ${node}`);

  const children = [];
  const markDefs: PortableTextMarkDefinition[] = [];

  for (const child of node.children) {
    if (Element.isElementType(child, "link")) {
      const [spans, markDef] = linkToPortableTextSpans(child);
      if (!markDefs.find((m) => m._key === markDef._key)) {
        markDefs.push(markDef);
      }
      children.push(...spans);
    } else {
      children.push(toPortableTextSpan(child));
    }
  }

  return {
    _type: "block",
    markDefs,
    children,
  };
};

const bulletedListBlocks = (node: BaseElement): PortableTextBlock[] => {
  return node.children.map((child) => ({
    ...toPortableTextBlock(child),
    listItem: "bullet",
  }));
};

function findMark(span: BaseText) {
  const marks = [];
  if (span.bold) {
    marks.push("strong");
  }
  if (span.italic) {
    marks.push("em");
  }
  return marks.length ? marks : undefined;
}
