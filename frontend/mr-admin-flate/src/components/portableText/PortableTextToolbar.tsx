import { BulletListIcon, LinkIcon, NumberListIcon } from "@navikt/aksel-icons";
import { Button, HStack, Modal, TextField, Tooltip } from "@navikt/ds-react";
import { AnnotationSchemaType, useEditor, useEditorSelector } from "@portabletext/editor";
import * as selectors from "@portabletext/editor/selectors";
import {
  ToolbarDecoratorSchemaType,
  ToolbarListSchemaType,
  useDecoratorButton,
  useToolbarSchema,
} from "@portabletext/toolbar";
import { FormEvent, FormEventHandler, RefObject, useRef, useState } from "react";
import { SupportedDecorator, SupportedList } from "./helper";

export function PortableTextEditorToolbar() {
  const ref = useRef<HTMLDialogElement>(null);
  const toolBarSchema = useToolbarSchema({});

  const decoratorButtons = toolBarSchema.decorators?.map((decorator) => (
    <DecoratorButton key={decorator.name} schemaType={decorator} />
  ));

  const annotationButtons = toolBarSchema.annotations?.map((annotation) => (
    <AnnotationButton key={annotation.name} modalRef={ref} schemaType={annotation} />
  ));

  const listButtons = toolBarSchema.lists?.map((list) => (
    <ListButton key={list.name} schemaType={list} />
  ));

  return (
    <>
      <HStack gap="2" className="border-t border-l border-r rounded-t-md">
        {decoratorButtons}
        {annotationButtons}
        {listButtons}
      </HStack>
      <LinkModal modalRef={ref} />
    </>
  );
}

function ToolbarButton({
  tooltipText,
  onClick,
  isActive,
  children,
}: {
  tooltipText: string;
  onClick: () => void;
  isActive: boolean;
  children: React.ReactNode;
}) {
  const tooltipContent = tooltipText.charAt(0).toUpperCase() + tooltipText.slice(1);
  return (
    <Tooltip content={tooltipContent}>
      <Button
        variant={isActive ? "primary-neutral" : "tertiary-neutral"}
        onClick={(e) => {
          e.preventDefault();
          e.stopPropagation();
          onClick();
        }}
      >
        {children}
      </Button>
    </Tooltip>
  );
}

function decoratorButtonContent(schemaName: string) {
  switch (schemaName) {
    case SupportedDecorator.STRONG:
      return <b className="text-1.5em">B</b>;
    case SupportedDecorator.EM:
      return <em className="text-1.5em">I</em>;
    case SupportedDecorator.UNDERLINE:
      return <span className="text-1.5em underline">U</span>;
    default:
      return <span>UNKNOWN</span>;
  }
}

function decoratorTooltipText(schemaName: string) {
  switch (schemaName) {
    case SupportedDecorator.STRONG:
      return "Bold";
    case SupportedDecorator.EM:
      return "Itallic";
    case SupportedDecorator.UNDERLINE:
      return "Underline";
    default:
      return "Unknown";
  }
}
function DecoratorButton({ schemaType }: { schemaType: ToolbarDecoratorSchemaType }) {
  const decoratorContext = useDecoratorButton({ schemaType: schemaType });
  const active = decoratorContext.snapshot.matches({ enabled: "active" });

  return (
    <ToolbarButton
      tooltipText={decoratorTooltipText(schemaType.name)}
      isActive={active}
      onClick={() => {
        decoratorContext.send({
          type: "toggle",
        });
      }}
    >
      {decoratorButtonContent(schemaType.name)}
    </ToolbarButton>
  );
}

function AnnotationButton({
  schemaType,
  modalRef,
}: {
  schemaType: AnnotationSchemaType;
  modalRef: RefObject<HTMLDialogElement | null>;
}) {
  const editor = useEditor();
  const active = useEditorSelector(editor, selectors.isActiveAnnotation(schemaType.name));

  function onClick() {
    if (active) {
      editor.send({
        type: "annotation.remove",
        annotation: {
          name: schemaType.name,
        },
      });
      editor.send({ type: "focus" });
    } else {
      modalRef.current?.showModal();
    }
  }
  return (
    <ToolbarButton tooltipText={schemaType.name} isActive={active} onClick={onClick}>
      <LinkIcon title="a11y-title" fontSize="1.2em" />
    </ToolbarButton>
  );
}

function LinkModal({ modalRef }: { modalRef: RefObject<HTMLDialogElement | null> }) {
  const editor = useEditor();
  const inputName = "lenke";
  const clickHandler = (form: FormData) => {
    const href = form.get(inputName);
    editor.send({
      type: "annotation.add",
      annotation: {
        name: "link",
        value: { href },
      },
    });
    modalRef.current?.close();
    editor.send({ type: "focus" });
  };
  return (
    <Modal ref={modalRef} header={{ heading: "Skriv inn lenke" }} width={400} portal>
      <Modal.Body>
        <form
          method="dialog"
          id="skjema"
          onSubmit={(e: FormEvent<HTMLFormElement>) => {
            e.preventDefault();
            e.stopPropagation();
            clickHandler(new FormData(e.currentTarget));
            e.currentTarget.reset();
          }}
        >
          <TextField label="Lenke" name={inputName} />
        </form>
      </Modal.Body>
      <Modal.Footer>
        <Button form="skjema" type="submit">
          Bekreft
        </Button>
        <Button type="button" variant="secondary" onClick={() => modalRef.current?.close()}>
          Avbryt
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

function getLinstButtonIcon(schemaTypeName: string): React.ReactNode {
  switch (schemaTypeName) {
    case SupportedList.BULLET:
      return <BulletListIcon aria-label="Bullet list" fontSize="1.2em" />;
    case SupportedList.NUMBER:
      return <NumberListIcon title="Number list" fontSize="1.2em" />;
    default:
      return null;
  }
}

function ListButton({ schemaType }: { schemaType: ToolbarListSchemaType }) {
  const editor = useEditor();
  const active = useEditorSelector(editor, selectors.isActiveListItem(schemaType.name));

  function onClick() {
    editor.send({
      type: "list item.toggle",
      listItem: schemaType.name,
    });
    editor.send({ type: "focus" });
  }

  return (
    <ToolbarButton tooltipText={schemaType.name + " list"} isActive={active} onClick={onClick}>
      {getLinstButtonIcon(schemaType.name)}
    </ToolbarButton>
  );
}
