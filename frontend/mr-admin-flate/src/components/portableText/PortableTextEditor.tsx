import { BulletListIcon, LinkIcon } from "@navikt/aksel-icons";
import type { PortableTextBlock } from "@portabletext/types";
import classnames from "classnames";
import isHotkey from "is-hotkey";
import isUrl from "is-url";
import React, { ForwardedRef, useCallback, useMemo } from "react";
import { Controller } from "react-hook-form";
import { Editor, Range, Element as SlateElement, Transforms, createEditor } from "slate";
import { withHistory } from "slate-history";
import { Editable, Slate, useSlate, withReact } from "slate-react";
import { portableTextToSlate } from "./portableTextToSlate";
import { slateToPortableText } from "./slateToPortableText";
import { FeilmeldingMedVarselTrekant } from "../skjema/FeilmeldingMedVarseltrekant";

const HOTKEYS: {
  [name: string]: string;
} = {
  "mod+b": "bold",
  "mod+i": "italic",
};

const LIST_TYPES = ["numbered-list", "bulleted-list"];

interface PortableTextEditorProps {
  size?: "small" | "medium";
  label: string;
  hideLabel?: boolean;
  description?: string;
  onChange?: (a0: {
    target: {
      value: PortableTextBlock[];
      name?: string;
    };
  }) => void;
  placeholder?: string;
  value?: PortableTextBlock[];
}

const emptySlateValue = [
  {
    type: "paragraph",
    children: [{ text: "" }],
  },
];

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function PortableTextEditor(props: PortableTextEditorProps, _: ForwardedRef<HTMLElement>) {
  const {
    placeholder,
    onChange: providedOnChange,
    label,
    hideLabel = false,
    size,
    description,
    ...rest
  } = props;
  const [focused, setFocused] = React.useState(false);
  const renderElement = useCallback((props: any) => <Element {...props} />, []);
  const renderLeaf = useCallback((props: any) => <Leaf {...props} />, []);
  const editor = useMemo(() => withLinks(withHistory(withReact(createEditor()))), []);

  return (
    <>
      <Controller
        name={label}
        {...rest}
        render={({ field: { onChange, value, name }, fieldState: { error } }) => {
          return (
            <div className="flex flex-col">
              <label
                className={classnames("inline-block", {
                  "navds-sr-only": hideLabel,
                })}
                style={{
                  fontSize: size === "small" ? "16px" : "18px",
                }}
                htmlFor={name}
              >
                <b>{label}</b>
              </label>
              {description && (
                <label
                  className={classnames("mb-[8px] inline-block text-text-subtle", {
                    "navds-sr-only": hideLabel,
                  })}
                  style={{
                    fontSize: size === "small" ? "16px" : "18px",
                  }}
                >
                  {description}
                </label>
              )}
              <Slate
                editor={editor}
                initialValue={
                  value && value.length > 0 ? portableTextToSlate(value) : emptySlateValue
                }
                onChange={(value) => {
                  const portableText = slateToPortableText(value);
                  onChange(portableText);
                  providedOnChange?.({
                    target: { value: portableText },
                  });
                }}
              >
                <div
                  className={classnames(
                    "border border-border-default rounded-lg flex flex-col hover:border-border-action",
                    focused && "outline-none shadow-focus",
                  )}
                >
                  <ToolbarComponent>
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
                    <BlockButton
                      format="bulleted-list"
                      icon={
                        <div>
                          <BulletListIcon aria-label="Bullet" /> Bullet
                        </div>
                      }
                    />
                    <LinkButton />
                  </ToolbarComponent>
                  <Editable
                    className="outline-none min-h-[200px] p-2 prose"
                    renderElement={renderElement}
                    renderLeaf={renderLeaf}
                    placeholder={placeholder}
                    onFocusCapture={() => setFocused(true)}
                    onFocus={() => setFocused(true)}
                    onBlur={() => setFocused(false)}
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
                </div>
              </Slate>
              {error && (
                <div className="mt-[8px] text-nav-red">
                  <FeilmeldingMedVarselTrekant>{error.message}</FeilmeldingMedVarselTrekant>
                </div>
              )}
            </div>
          );
        }}
      />
    </>
  );
}

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
        const url = window.prompt("Legg til lenke");
        if (!url) return;

        if (!isUrl(url)) {
          alert("Ugyldig lenke. Husk `https://` foran");
        } else {
          insertLink(editor, url);
        }
        // Flytter cursor til slutten for å deaktivere link modus
        Transforms.select(editor, Editor.end(editor, []));
      }}
    >
      <div>
        <LinkIcon aria-label="Link" /> Link
      </div>
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

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { active, children, ...props },
  ref,
) {
  return (
    <span
      {...props}
      ref={ref}
      style={{
        cursor: "pointer",
        color: active ? "black" : "#676F7B",
      }}
    >
      {children}
    </span>
  );
});

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function Toolbar({ children, ...props }: { children: any }, _: ForwardedRef<HTMLElement>) {
  return (
    <div
      {...props}
      className="flex flex-row justify-start gap-8 border-b border-b-border-default [&>span>div]:flex [&>span>div]:items-center [&>span>div]:justify-center [&>span>div]:gap-2 [&>span>div]:p-[10px] hover:[&>span]:bg-gray-200 hover:[&>span]:!text-text-default hover:[&>span]:rounded-sm"
    >
      {children}
    </div>
  );
}

const ToolbarComponent = React.forwardRef(Toolbar);

const PortableTextEditorComponent = React.forwardRef(PortableTextEditor);

export { PortableTextEditorComponent as PortableTextEditor };
