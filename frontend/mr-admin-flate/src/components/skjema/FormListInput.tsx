import { ArrowDownIcon, ArrowUpIcon, PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { Button, HStack, VStack } from "@navikt/ds-react";
import type { ReactNode } from "react";
import { useFieldArray, useFormContext } from "react-hook-form";

interface Props {
  name: string;
  addButtonLabel: string;
  emptyItem: Record<string, unknown> | (() => Record<string, unknown>);
  renderItem: (index: number, id: string) => ReactNode;
}

export function FormListInput({ name, addButtonLabel, emptyItem, renderItem }: Props) {
  const { control } = useFormContext();
  const { fields, append, remove, move } = useFieldArray({ control, name });

  function newItem() {
    return typeof emptyItem === "function" ? emptyItem() : emptyItem;
  }

  return (
    <VStack gap="space-12">
      {fields.map((field, index) => (
        <HStack
          key={field.id}
          padding="space-16"
          gap="space-16"
          align="center"
          wrap={false}
          className="border-ax-border-neutral-subtle border rounded-lg"
        >
          <VStack gap="space-8" className="flex-1">
            {renderItem(index, field.id)}
          </VStack>
          <HStack gap="space-4" align="center">
            <VStack>
              {index > 0 && (
                <Button
                  type="button"
                  size="small"
                  variant="tertiary-neutral"
                  icon={<ArrowUpIcon aria-hidden />}
                  onClick={() => move(index, index - 1)}
                  aria-label="Flytt opp"
                />
              )}
              {index < fields.length - 1 && (
                <Button
                  type="button"
                  size="small"
                  variant="tertiary-neutral"
                  icon={<ArrowDownIcon aria-hidden />}
                  onClick={() => move(index, index + 1)}
                  aria-label="Flytt ned"
                />
              )}
            </VStack>
            <Button
              type="button"
              size="small"
              variant="tertiary"
              data-color="danger"
              icon={<TrashIcon aria-hidden />}
              onClick={() => remove(index)}
              aria-label="Fjern"
            />
          </HStack>
        </HStack>
      ))}
      <Button
        className="self-start"
        variant="tertiary"
        size="small"
        type="button"
        icon={<PlusIcon aria-hidden />}
        onClick={() => append(newItem())}
      >
        {addButtonLabel}
      </Button>
    </VStack>
  );
}
