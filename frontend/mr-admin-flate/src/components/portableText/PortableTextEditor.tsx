import React, { useCallback, useMemo } from "react";
import isUrl from "is-url";
import { LinkIcon, BulletListIcon } from "@navikt/aksel-icons";

import isHotkey from "is-hotkey";
import { Editable, withReact, useSlate, Slate } from "slate-react";
import { Element as SlateElement, Editor, Transforms, createEditor, Range } from "slate";
import { withHistory } from "slate-history";
import type { PortableTextBlock } from "@portabletext/types";
import { slateToPortableText } from "./slateToPortableText";
import { portableTextToSlate } from "./portableTextToSlate";

const HOTKEYS: {
  [name: string]: string;
} = {
  "mod+b": "bold",
  "mod+i": "italic",
};

const LIST_TYPES = ["numbered-list", "bulleted-list"];

interface PortableTextEditorProps {
  onChange: (arg0: any) => void;
  initialValue: PortableTextBlock[];
}

export const PortableTextEditor = (props: PortableTextEditorProps) => {
  const renderElement = useCallback((props: any) => <Element {...props} />, []);
  const renderLeaf = useCallback((props: any) => <Leaf {...props} />, []);
  const editor = useMemo(() => withLinks(withHistory(withReact(createEditor()))), []);
  const initialValue =
    props.initialValue.length > 0
      ? portableTextToSlate(props.initialValue)
      : [
          {
            type: "paragraph",
            children: [{ text: "" }],
          },
        ];

  return (
    <>
      <Slate
        editor={editor}
        initialValue={initialValue}
        onChange={(value) => {
          if (value) {
            props.onChange(slateToPortableText(value));
          }
        }}
      >
        <Toolbar>
          <LinkButton />
          <MarkButton
            format="bold"
            icon={
              <div>
                <b>B</b> bold
              </div>
            }
          />
          <MarkButton
            format="italic"
            icon={
              <div>
                <em>I</em> italic
              </div>
            }
          />
          <BlockButton format="heading-one" icon={<div>Heading</div>} />
          <BlockButton format="bulleted-list" icon={<BulletListIcon />} />
        </Toolbar>
        <Editable
          renderElement={renderElement}
          renderLeaf={renderLeaf}
          placeholder="Enter some rich text…"
          autoFocus
          onKeyDown={(event) => {
            for (const hotkey in HOTKEYS) {
              if (isHotkey(hotkey, event)) {
                event.preventDefault();
                const mark = HOTKEYS[hotkey];
                toggleMark(editor, mark);
              }
            }
          }}
        />
      </Slate>
    </>
  );
};

const withLinks = (editor: any) => {
  const { insertData, insertText, isInline } = editor;

  editor.isInline = (element: any) => {
    return element.type === "link" ? true : isInline(element);
  };

  editor.insertText = (text: any) => {
    if (text && isUrl(text)) {
      wrapLink(editor, text);
    } else {
      insertText(text);
    }
  };

  editor.insertData = (data: any) => {
    const text = data.getData("text/plain");

    if (text && isUrl(text)) {
      wrapLink(editor, text);
    } else {
      insertData(data);
    }
  };

  return editor;
};

const insertLink = (editor: any, url: string) => {
  if (editor.selection) {
    wrapLink(editor, url);
  }
};

const LinkButton = () => {
  const editor = useSlate();
  return (
    <Button
      active={isLinkActive(editor)}
      onMouseDown={(event: any) => {
        event.preventDefault();
        const url = window.prompt("Enter the URL of the link:");
        if (!url) return;
        insertLink(editor, url);
      }}
    >
      <LinkIcon />
    </Button>
  );
};

const isLinkActive = (editor: Editor) => {
  const [link] = Editor.nodes(editor, {
    match: (n) => SlateElement.isElement(n) && n.type === "link",
  });
  return !!link;
};

const unwrapLink = (editor: Editor) => {
  Transforms.unwrapNodes(editor, {
    match: (n) => SlateElement.isElement(n) && n.type === "link",
  });
};

const wrapLink = (editor: Editor, url: string) => {
  if (isLinkActive(editor)) {
    unwrapLink(editor);
  }

  const { selection } = editor;
  const isCollapsed = selection && Range.isCollapsed(selection);
  const link = {
    type: "link",
    url,
    children: isCollapsed ? [{ text: url }] : [],
  };

  if (isCollapsed) {
    Transforms.insertNodes(editor, link);
  } else {
    Transforms.wrapNodes(editor, link, {
      split: true,
    });
    Transforms.collapse(editor, {
      edge: "end",
    });
  }
};

const toggleBlock = (editor: Editor, format: string) => {
  const isActive = isBlockActive(editor, format);
  const isList = LIST_TYPES.includes(format);

  Transforms.unwrapNodes(editor, {
    match: (n) => SlateElement.isElement(n) && LIST_TYPES.includes(n.type),
    split: true,
  });

  Transforms.setNodes(editor, {
    type: isActive ? "paragraph" : isList ? "list-item" : format,
  });

  if (!isActive && isList) {
    const block = {
      type: format,
      children: [],
    };
    Transforms.wrapNodes(editor, block);
  }
};

const toggleMark = (editor: Editor, format: string) => {
  const isActive = isMarkActive(editor, format);

  if (isActive) {
    Editor.removeMark(editor, format);
  } else {
    Editor.addMark(editor, format, true);
  }
};

const isBlockActive = (editor: Editor, format: string): boolean => {
  const [match] = Editor.nodes(editor, {
    match: (n) => SlateElement.isElement(n) && n.type === format,
  });

  return !!match;
};

const isMarkActive = (editor: Editor, format: string): boolean => {
  const marks: {
    [name: string]: boolean;
  } | null = Editor.marks(editor);
  return marks ? marks[format] === true : false;
};

const Element = ({
  attributes,
  children,
  element,
}: {
  attributes: any;
  children: React.ReactNode;
  element: any;
}) => {
  switch (element.type) {
    case "block-quote":
      return <blockquote {...attributes}>{children}</blockquote>;
    case "bulleted-list":
      return <ul {...attributes}>{children}</ul>;
    case "heading-one":
      return <h1 {...attributes}>{children}</h1>;
    case "heading-two":
      return <h2 {...attributes}>{children}</h2>;
    case "list-item":
      return <li {...attributes}>{children}</li>;
    case "numbered-list":
      return <ol {...attributes}>{children}</ol>;
    case "link":
      return (
        <a {...attributes} href={element.url}>
          {children}
        </a>
      );
    default:
      return <p {...attributes}>{children}</p>;
  }
};

const Leaf = ({
  attributes,
  children,
  leaf,
}: {
  attributes: any;
  children: React.ReactNode;
  leaf: any;
}) => {
  if (leaf.bold) {
    children = <strong>{children}</strong>;
  }

  if (leaf.code) {
    children = <code>{children}</code>;
  }

  if (leaf.italic) {
    children = <em>{children}</em>;
  }

  if (leaf.underline) {
    children = <u>{children}</u>;
  }

  return <span {...attributes}>{children}</span>;
};

const BlockButton = ({ format, icon }: { format: string; icon: React.ReactNode }) => {
  const editor = useSlate();
  return (
    <Button
      active={isBlockActive(editor, format)}
      onMouseDown={(event) => {
        event.preventDefault();
        toggleBlock(editor, format);
      }}
    >
      {icon}
    </Button>
  );
};

const MarkButton = ({ format, icon }: { format: string; icon: React.ReactNode }) => {
  const editor = useSlate();
  return (
    <Button
      active={isMarkActive(editor, format)}
      onMouseDown={(event) => {
        event.preventDefault();
        toggleMark(editor, format);
      }}
    >
      {icon}
    </Button>
  );
};

type ButtonProps = React.HTMLProps<HTMLButtonElement> & {
  active: boolean;
};

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ active, children, ...props }, ref) => (
    <span
      {...props}
      ref={ref}
      style={{
        cursor: "pointer",
        color: active ? "black" : "#ccc",
      }}
    >
      {children}
    </span>
  ),
);
Button.displayName = "Button";

export const Toolbar = React.forwardRef<HTMLDivElement, { children: React.ReactNode }>(
  ({ children, ...props }, ref) => (
    <div
      {...props}
      ref={ref}
      style={{
        marginLeft: "15px",
        display: "flex",
        flexDirection: "row",
        justifyContent: "space-around",
        position: "relative",
        padding: "1px 18px 17px",
        margin: "0 -20px",
        borderBottom: "2px solid #eee",
        marginBottom: "20px",
      }}
    >
      {children}
    </div>
  ),
);
Toolbar.displayName = "Toolbar";
