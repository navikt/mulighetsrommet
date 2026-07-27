import { ChevronDownIcon } from "@navikt/aksel-icons";
import { Checkbox, CheckboxGroup, HStack, Box } from "@navikt/ds-react";
import { useState } from "react";
import { addOrRemove, addOrRemoveBy } from "../../utils/utils";

export interface CheckboxGroupProps {
  value: string[];
  onChange: (value: string[]) => void;
  items: CheckboxGroupItem[];
  legend: string;
}

export interface CheckboxGroupItem {
  id: string;
  navn: string;
  items?: Array<CheckboxGroupSubItem>;
}

export interface CheckboxGroupSubItem {
  navn: string;
  id: string;
  erStandardvalg: boolean;
}

export function CheckboxDropdownGroup({ value, onChange, items, legend }: CheckboxGroupProps) {
  const [groupOpen, setGroupOpen] = useState<string[]>([]);

  function itemOnChange(id: string) {
    onChange(addOrRemoveBy(value, id, (a, b) => a === b));
  }

  return (
    <CheckboxGroup legend={legend} hideLegend>
      {items.map(({ id, navn, items }) => {
        if (!items || items.length === 0) {
          return (
            <Checkbox
              size="small"
              key={id}
              checked={itemIsChecked(value, id)}
              onChange={() => itemOnChange(id)}
            >
              {navn}
            </Checkbox>
          );
        }

        return (
          <Box key={id}>
            <HStack
              justify="space-between"
              align="center"
              width="100%"
              wrap={false}
              className="group hover:bg-ax-bg-accent-moderate-hover rounded"
            >
              <Checkbox
                size="small"
                key={id}
                checked={isChecked(value, items)}
                indeterminate={isIndeterminate(value, items)}
                onChange={() => onChange(toggleSelected(value, items))}
                className="text-start"
              >
                {navn}
              </Checkbox>
              <HStack
                as="button"
                onClick={() => setGroupOpen([...addOrRemove(groupOpen, id)])}
                aria-expanded={groupOpen.includes(id)}
                aria-controls={`subgroup-${id}`}
                className="cursor-pointer flex-1 justify-end"
                aria-label={`${groupOpen.includes(id) ? "Lukk" : "Åpne"} undergruppe for ${navn}`}
              >
                <Box
                  borderRadius="8"
                  padding="space-1"
                  background="accent-moderateA"
                  className="text-ax-text-accent-subtle group-hover:bg-ax-bg-accent-strong-hover group-hover:text-ax-text-neutral-contrast"
                >
                  <ChevronDownIcon
                    aria-hidden
                    fontSize="1.25rem"
                    className={`transition-transform ease-in-out duration-75 ${groupOpen.includes(id) ? "rotate-180" : "rotate-0"}`}
                  />
                </Box>
              </HStack>
            </HStack>
            <Box
              marginInline="space-8 space-0"
              marginBlock="space-8"
              paddingInline="space-20 space-0"
              borderColor="neutral-subtle"
              borderWidth="0 0 0 2"
              id={`subgroup-${id}`}
              hidden={!groupOpen.includes(id)}
            >
              {items.map(({ id, navn }) => (
                <Checkbox
                  checked={itemIsChecked(value, id)}
                  onChange={() => itemOnChange(id)}
                  key={id}
                  size="small"
                  className=""
                >
                  {navn}
                </Checkbox>
              ))}
            </Box>
          </Box>
        );
      })}
    </CheckboxGroup>
  );
}

function toggleSelected(value: string[], items: CheckboxGroupSubItem[]) {
  const currentlySelectedInGroup = getSelectedItems(value, items);

  return currentlySelectedInGroup.length > 0
    ? value.filter((id) => !currentlySelectedInGroup.includes(id))
    : Array.from(
        new Set(
          items
            .filter((item) => item.erStandardvalg)
            .map((item) => item.id)
            .concat(value),
        ),
      );
}

function getSelectedItems(value: string[], items: CheckboxGroupSubItem[]): string[] {
  return value.filter((id) => items.some((item) => item.id === id));
}

function isIndeterminate(value: string[], items: CheckboxGroupSubItem[]): boolean {
  const count = getSelectedItems(value, items).length;
  return count > 0 && count < items.length;
}

function isChecked(value: string[], items: CheckboxGroupSubItem[]): boolean {
  return getSelectedItems(value, items).length === items.length;
}

function itemIsChecked(value: string[], id: string): boolean {
  return value.includes(id);
}
