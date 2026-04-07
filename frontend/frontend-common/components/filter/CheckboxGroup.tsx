import { ChevronDownIcon } from "@navikt/aksel-icons";
import {
  Checkbox,
  CheckboxGroup as AkselCheckboxGroup,
  CheckboxGroupProps as AkselCheckboxGroupProps,
} from "@navikt/ds-react";
import classnames from "classnames";
import { useState } from "react";
import styles from "./CheckboxGroup.module.scss";
import { addOrRemove, addOrRemoveBy } from "../../utils/utils";

export interface CheckboxGroupProps extends Omit<AkselCheckboxGroupProps, "children"> {
  value: string[];
  onChange: (value: string[]) => void;
  items: CheckboxGroupItem[];
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

export function CheckboxGroup({ value, onChange, items, ...props }: CheckboxGroupProps) {
  const [groupOpen, setGroupOpen] = useState<string[]>([]);

  function itemOnChange(id: string) {
    onChange(addOrRemoveBy(value, id, (a, b) => a === b));
  }

  return (
    <AkselCheckboxGroup {...props}>
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
          <div key={id}>
            <div
              className={styles.checkbox_and_caret}
              onClick={() => setGroupOpen([...addOrRemove(groupOpen, id)])}
            >
              <div onClick={(e) => e.stopPropagation()} className={styles.checkbox}>
                <Checkbox
                  size="small"
                  key={id}
                  checked={isChecked(value, items)}
                  indeterminate={isIndeterminate(value, items)}
                  onChange={() => onChange(toggleSelected(value, items))}
                >
                  {navn}
                </Checkbox>
              </div>
              <div className={styles.caret_container}>
                <ChevronDownIcon
                  aria-label="Ikon ned"
                  fontSize="1.25rem"
                  className={classnames(styles.accordion_down, {
                    [styles.accordion_down_open]: groupOpen.includes(id),
                  })}
                />
              </div>
            </div>
            {groupOpen.includes(id) && (
              <div style={{ marginLeft: "1rem" }}>
                {items.map(({ id, navn }) => (
                  <Checkbox
                    checked={itemIsChecked(value, id)}
                    onChange={() => itemOnChange(id)}
                    key={id}
                    size="small"
                  >
                    {navn}
                  </Checkbox>
                ))}
              </div>
            )}
          </div>
        );
      })}
    </AkselCheckboxGroup>
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
