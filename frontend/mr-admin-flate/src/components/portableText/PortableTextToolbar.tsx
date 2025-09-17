import { Button, HStack, Tooltip } from "@navikt/ds-react";
import {
  ToolbarDecoratorSchemaType,
  ToolbarListSchemaType,
  useDecoratorButton,
  useListButton,
  useToolbarSchema,
} from "@portabletext/toolbar";
import { SupportedDecorator, SupportedList } from "./helper";
import { BulletListIcon } from "@navikt/aksel-icons";

function ListButton(props: { schemaType: ToolbarListSchemaType }) {
  const decoratorButton = useListButton(props);
  let tooltipText: string;
  let icon: React.ReactNode;
  switch (props.schemaType.name) {
    case SupportedList.BULLET: {
      tooltipText = "Bullets";
      icon = <BulletListIcon aria-label="Bullets" />;
      break;
    }
    default:
      return null;
  }
  return (
    <ToolbarButton
      tooltipContent={tooltipText}
      buttonContent={icon}
      isActive={decoratorButton.snapshot.matches({ enabled: "active" })}
      onClick={() => decoratorButton.send({ type: "toggle" })}
    />
  );
}

function DecoratorButton(props: { schemaType: ToolbarDecoratorSchemaType }) {
  const decoratorButton = useDecoratorButton(props);
  let tooltipText: string;
  let icon: React.ReactNode;
  switch (props.schemaType.name) {
    case SupportedDecorator.STRONG: {
      tooltipText = "Bold";
      icon = <b>B</b>;
      break;
    }
    case SupportedDecorator.EM: {
      tooltipText = "Italic";
      icon = <em>I</em>;
      break;
    }
    default:
      return null;
  }
  return (
    <ToolbarButton
      tooltipContent={tooltipText}
      buttonContent={icon}
      isActive={decoratorButton.snapshot.matches({ enabled: "active" })}
      onClick={() => decoratorButton.send({ type: "toggle" })}
    />
  );
}

interface ToolbarButtonProps {
  tooltipContent: string;
  buttonContent: React.ReactNode;
  isActive: boolean;
  onClick: () => void;
}

function ToolbarButton({ tooltipContent, buttonContent, isActive, onClick }: ToolbarButtonProps) {
  return (
    <Tooltip content={tooltipContent}>
      <Button
        size="small"
        onClick={(e) => {
          e.preventDefault();
          e.stopPropagation();
          onClick();
        }}
        variant={isActive ? "secondary" : "secondary-neutral"}
      >
        {buttonContent}
      </Button>
    </Tooltip>
  );
}

export function PortableTextToolbar() {
  const toolBarSchema = useToolbarSchema({});
  return (
    <HStack gap="2" className={""}>
      {toolBarSchema.decorators?.map((decorator) => (
        <DecoratorButton key={decorator.name} schemaType={decorator} />
      ))}
      {toolBarSchema.lists?.map((list) => (
        <ListButton key={list.name} schemaType={list} />
      ))}
    </HStack>
  );
}
