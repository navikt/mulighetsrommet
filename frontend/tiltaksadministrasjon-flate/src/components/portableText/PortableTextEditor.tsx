import {
  defineAnnotation,
  defineDecorator,
  defineSchema,
  defineTextBlock,
  EditorProvider,
  PortableTextBlock,
  PortableTextEditable,
  RegistrableNode,
  SchemaDefinition,
  TextBlockRenderProps,
} from "@portabletext/editor";
import { EventListenerPlugin, NodePlugin } from "@portabletext/editor/plugins";
import { ListIndexProvider, useListIndex } from "@portabletext/plugin-list-index";
import { BodyLong, Link, Tooltip } from "@navikt/ds-react";
import "./portableTextEditor.css";
import { PortableTextEditorToolbar } from "./PortableTextToolbar";
import { Controller, useFormContext } from "react-hook-form";
import { SupportedAnnotation, SupportedDecorator, SupportedList } from "./helper";

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

const nodes: RegistrableNode[] = [
  defineTextBlock({
    type: "block",
    render: (props) => {
      if (props.node.listItem) {
        return (
          <ListItemBlock attributes={props.attributes} node={props.node} path={props.path}>
            {props.children}
          </ListItemBlock>
        );
      }
      return (
        <BodyLong size="small" className="mb-1 min-h-3" {...props.attributes}>
          {props.children}
        </BodyLong>
      );
    },
  }),
  defineDecorator({
    type: SupportedDecorator.STRONG,
    render: (props) => <strong>{props.children}</strong>,
  }),
  defineDecorator({
    type: SupportedDecorator.EM,
    render: (props) => <em>{props.children}</em>,
  }),
  defineDecorator({
    type: SupportedDecorator.UNDERLINE,
    render: (props) => <u>{props.children}</u>,
  }),
  defineAnnotation({
    type: SupportedAnnotation.LINK,
    render: (props) => {
      const href = typeof props.annotation.href === "string" ? props.annotation.href : "";
      return (
        <Tooltip content={href}>
          <Link href={href}>{props.children}</Link>
        </Tooltip>
      );
    },
  }),
];

function ListItemBlock(props: {
  attributes: TextBlockRenderProps["attributes"];
  node: TextBlockRenderProps["node"];
  path: TextBlockRenderProps["path"];
  children: TextBlockRenderProps["children"];
}) {
  const listIndex = useListIndex(props.path);
  return (
    <div
      {...props.attributes}
      data-list-item={props.node.listItem}
      data-level={props.node.level}
      data-list-index={listIndex}
    >
      {props.children}
    </div>
  );
}

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
            <label className={"mb-2 inline-block text-ax-text-neutral-subtle"}>{description}</label>
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
  return (
    <>
      <EditorProvider
        initialConfig={{
          schemaDefinition: schema,
          initialValue: value,
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
        <ListIndexProvider>
          <NodePlugin nodes={nodes} />
          <PortableTextEditable className="p-2 border rounded-b-md" />
        </ListIndexProvider>
      </EditorProvider>
    </>
  );
}
