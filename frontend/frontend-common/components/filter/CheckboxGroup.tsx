import { ChevronDownIcon } from "@navikt/aksel-icons";
import { Checkbox } from "@navikt/ds-react";
import classnames from "classnames";
import { useState } from "react";
import styles from "./CheckboxGroup.module.scss";
import { addOrRemove, addOrRemoveBy } from "../../utils/utils";

export interface CheckboxGroup {
  id: string;
  navn: string;
  items: Array<CheckboxGroupItem>;
}

export interface CheckboxGroupItem {
  navn: string;
  id: string;
  erStandardvalg: boolean;
}

interface CheckboxGroupProps {
  value: string[];
  onChange: (value: string[]) => void;
  groups: CheckboxGroup[];
}

export function CheckboxGroup({ value, onChange, groups }: CheckboxGroupProps) {
  const [groupOpen, setGroupOpen] = useState<string[]>([]);

  function getSelectedItems(group: CheckboxGroup): string[] {
    return value.filter((id) => group.items.some((item) => item.id === id));
  }

  function groupIsIndeterminate(group: CheckboxGroup): boolean {
    const count = getSelectedItems(group).length;
    return count > 0 && count < group.items.length;
  }

  function groupIsChecked(group: CheckboxGroup): boolean {
    return getSelectedItems(group).length === group.items.length;
  }

  function groupOnChange(group: CheckboxGroup) {
    const currentlySelectedInGroup = getSelectedItems(group);

    const nextValue =
      currentlySelectedInGroup.length > 0
        ? value.filter((id) => !currentlySelectedInGroup.includes(id))
        : Array.from(
            new Set(
              group.items
                .filter((item) => item.erStandardvalg)
                .map((item) => item.id)
                .concat(value),
            ),
          );

    onChange(nextValue);
  }

  function itemIsChecked(id: string): boolean {
    return value.includes(id);
  }

  function itemOnChange(id: string) {
    onChange(addOrRemoveBy(value, id, (a, b) => a === b));
  }

  return (
    <>
      {groups.map((group: CheckboxGroup) => (
        <div key={group.id}>
          <div
            className={styles.checkbox_and_caret}
            onClick={() => setGroupOpen([...addOrRemove(groupOpen, group.id)])}
          >
            <div onClick={(e) => e.stopPropagation()} className={styles.checkbox}>
              <Checkbox
                size="small"
                key={group.id}
                checked={groupIsChecked(group)}
                onChange={() => groupOnChange(group)}
                indeterminate={groupIsIndeterminate(group)}
              >
                {group.navn}
              </Checkbox>
            </div>
            <div className={styles.caret_container}>
              <ChevronDownIcon
                aria-label="Ikon ned"
                fontSize="1.25rem"
                className={classnames(styles.accordion_down, {
                  [styles.accordion_down_open]: groupOpen.includes(group.id),
                })}
              />
            </div>
          </div>
          {groupOpen.includes(group.id) && (
            <div style={{ marginLeft: "1rem" }}>
              {group.items.map(({ id, navn }) => (
                <Checkbox
                  checked={itemIsChecked(id)}
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
      ))}
    </>
  );
}
