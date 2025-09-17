import { defineSchema, EditorProvider, PortableTextEditable } from "@portabletext/editor";
import type {
  PortableTextBlock,
  RenderAnnotationFunction,
  RenderBlockFunction,
  RenderChildFunction,
  RenderDecoratorFunction,
  RenderListItemFunction,
  RenderStyleFunction,
  SchemaDefinition,
} from "@portabletext/editor";
import { EventListenerPlugin } from "@portabletext/editor/plugins";
import { useFormContext } from "react-hook-form";
import { PortableTextToolbar } from "./PortableTextToolbar";
import { SupportedAnnotation, SupportedDecorator, SupportedList, SupportedStyle } from "./helper";

interface PortableTextFormEditorProps {
  name: string;
  label: string;
  description?: string;
}

export function PortableTextFormEditor({ name, label, description }: PortableTextFormEditorProps) {
  const { register, setValue, getValues } = useFormContext();
  register(name);

  const value = getValues(name);
  const handleChange = (newValue: any) => {
    setValue(name, newValue, { shouldDirty: true });
  };
  return (
    <div className="flex flex-col">
      <label className={"inline-block"} htmlFor={name}>
        <b>{label}</b>
      </label>
      {description && (
        <label className={"mb-[8px] inline-block text-text-subtle"}>{description}</label>
      )}
      <PortableTextEditor value={value} onChange={handleChange} />
    </div>
  );
}

interface PortableTextEditorProps {
  value: PortableTextBlock[];
  onChange: (blocks: PortableTextBlock[] | undefined) => Promise<void> | void;
  schema?: SchemaDefinition;
}

export function PortableTextEditor({
  value,
  schema = defaultSchemaDefinition,
  onChange,
}: PortableTextEditorProps) {
  return (
    <EditorProvider
      initialConfig={{
        schemaDefinition: schema,
        initialValue: value,
      }}
    >
      <div style={{ border: "1px solid black", padding: "0.5em" }}>
        <EventListenerPlugin
          on={(event) => {
            if (event.type === "value changed" || event.type === "mutation") {
              onChange(event.value);
            }
          }}
        />
        <PortableTextToolbar />
        <PortableTextEditable
          renderDecorator={renderDecorator}
          renderStyle={renderStyle}
          renderAnnotation={renderAnnotation}
          renderListItem={renderListItem}
          renderBlock={renderBlock}
          renderChild={renderChild}
        />
      </div>
    </EditorProvider>
  );
}

const defaultSchemaDefinition = defineSchema({
  // Decorators are simple marks that don't hold any data
  decorators: [{ name: SupportedDecorator.STRONG }, { name: SupportedDecorator.EM }],
  // Styles apply to entire text blocks
  // There's always a 'normal' style that can be considered the paragraph style
  styles: [{ name: SupportedStyle.NORMAL }],

  // The types below are left empty for this example.
  // See the rendering guide to learn more about each type.

  // Annotations are more complex marks that can hold data (for example, hyperlinks).
  annotations: [{ name: SupportedAnnotation.HYPERLINKS }],
  // Lists apply to entire text blocks as well (for example, bullet, numbered).
  lists: [{ name: SupportedList.BULLET }],
  // Inline objects hold arbitrary data that can be inserted into the text (for example, custom emoji).
  inlineObjects: [],
  // Block objects hold arbitrary data that live side-by-side with text blocks (for example, images, code blocks, and tables).
  blockObjects: [],
});

const renderDecorator: RenderDecoratorFunction = (props) => {
  console.log("DECORATOR", props);
  if (props.value === SupportedDecorator.STRONG) {
    return <strong>{props.children}</strong>;
  }
  if (props.value === SupportedDecorator.EM) {
    return <em>{props.children}</em>;
  }
  return <>{props.children}</>;
};

const renderStyle: RenderStyleFunction = (props) => {
  console.log("STYLE", props);
  if (props.value === SupportedStyle.NORMAL) {
    return <p>{props.children}</p>;
  }
  return <>{props.children}</>;
};

const renderAnnotation: RenderAnnotationFunction = (props) => {
  console.log("ANNOTATION", props);
  if (props.schemaType.name === SupportedAnnotation.HYPERLINKS) {
    return <a>{props.children}</a>;
  }
  return <>{props.children}</>;
};

const renderListItem: RenderListItemFunction = (props) => {
  console.log("LIST_ITEM", props);
  if (props.value === SupportedList.BULLET) {
    return <ul>{props.children}</ul>;
  }
  return <>{props.children}</>;
};

const renderBlock: RenderBlockFunction = (props) => {
  console.log("BLOCK", props);
  if (props.listItem === SupportedList.BULLET) {
    return <ul>{props.children} </ul>;
  }
  return <div>{props.children}</div>;
};
const renderChild: RenderChildFunction = (props) => {
  console.log("CHILD", props);
  if (props.schemaType.name === "span") return <span>{props.children}</span>;
  return <>{props.children}</>;
};
