import {
  defineSchema,
  EditorProvider,
  PortableTextBlock,
  PortableTextEditable,
  RenderAnnotationFunction,
  RenderBlockFunction,
  RenderDecoratorFunction,
  RenderStyleFunction,
  SchemaDefinition,
} from "@portabletext/editor";
import { EventListenerPlugin } from "@portabletext/editor/plugins";
import { Link, Tooltip } from "@navikt/ds-react";
import "./portableTextEditor.css";
import { PortableTextEditorToolbar } from "./PortableTextToolbar2";
import { Controller, useFormContext } from "react-hook-form";
import {
  convertSlateToPortableText,
  SupportedAnnotation,
  SupportedDecorator,
  SupportedList,
} from "./helper";

// Define the schema for the editor
// All options are optional
// Only the `name` property is required, but you can define a `title` and an `icon` as well
// You can use this schema definition later to build your toolbar
const schemaDefinition = defineSchema({
  // Decorators are simple marks that don't hold any data
  decorators: [
    { name: SupportedDecorator.STRONG },
    { name: SupportedDecorator.EM },
    { name: SupportedDecorator.UNDERLINE },
  ],
  // Annotations are more complex marks that can hold data
  annotations: [{ name: SupportedAnnotation.LINK, fields: [{ name: "href", type: "string" }] }],
  // Styles apply to entire text blocks
  // There's always a 'normal' style that can be considered the paragraph style
  styles: [],
  lists: [{ name: SupportedList.BULLET }, { name: SupportedList.NUMBER }],
  inlineObjects: [],
  blockObjects: [],
});

interface PortableTextFormEditorProps {
  name: string;
  label: string;
  description?: string;
}

export function PortableTextFormEditor({ name, label, description }: PortableTextFormEditorProps) {
  const formContext = useFormContext();
  return (
    <Controller
      name={name}
      control={formContext.control}
      render={({ field, fieldState: { error } }) => (
        <div className="flex flex-col">
          <label className={"inline-block"} htmlFor={field.name}>
            <b>{label}</b>
          </label>
          {description && (
            <label className={"mb-[8px] inline-block text-text-subtle"}>{description}</label>
          )}
          <PortableTextEditor value={field.value} onChange={field.onChange} />
          {error && <span>{error.message}</span>}
        </div>
      )}
    />
  );
}

interface PortableTextEditorProps {
  value: PortableTextBlock[];
  onChange: (blocks: PortableTextBlock[] | undefined) => Promise<void> | void;
  schema?: SchemaDefinition;
}

export function PortableTextEditor({
  value,
  onChange,
  schema = schemaDefinition,
}: PortableTextEditorProps) {
  const slateConverted = convertSlateToPortableText(value);
  return (
    <>
      <EditorProvider
        initialConfig={{
          schemaDefinition: schema,
          initialValue: slateConverted,
        }}
      >
        <EventListenerPlugin
          on={(event) => {
            if (event.type === "mutation") {
              onChange(event.value);
            }
          }}
        />
        <PortableTextEditorToolbar />
        <PortableTextEditable
          className="p-[8px] border rounded-b-md"
          renderDecorator={renderDecorator}
          renderAnnotation={renderAnnotation}
          renderBlock={renderBlock}
          renderStyle={renderStyle}
          // Liste-elementer er egne "blocks", så kan ikke bruke Aksel her. See css i samme mappe.
          renderListItem={(props) => <>{props.children}</>}
        />
      </EditorProvider>
    </>
  );
}

const renderDecorator: RenderDecoratorFunction = (props) => {
  if (props.value === SupportedDecorator.STRONG) {
    return <strong>{props.children}</strong>;
  }
  if (props.value === SupportedDecorator.EM) {
    return <em>{props.children}</em>;
  }
  if (props.value === SupportedDecorator.UNDERLINE) {
    return <u>{props.children}</u>;
  }
  return <>{props.children}</>;
};

const renderAnnotation: RenderAnnotationFunction = (props) => {
  if (props.schemaType.name === SupportedAnnotation.LINK) {
    return (
      <Tooltip content={props.value.href as string}>
        <Link href={props.value.href as string}>{props.children}</Link>
      </Tooltip>
    );
  }

  return <>{props.children}</>;
};

// Empty line
const renderBlock: RenderBlockFunction = (props) => {
  return <div>{props.children}</div>;
};

const renderStyle: RenderStyleFunction = (props) => {
  return <>{props.children}</>;
};
