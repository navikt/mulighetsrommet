import React, { ChangeEvent } from 'react';
import { useController, UseControllerProps } from 'react-hook-form';
import { Select } from '@navikt/ds-react';

export interface SelectProps<T extends object, V> extends UseControllerProps<T> {
  label: string;
  placeholder: string;
  /**
   * When set, the value produced by selecting the placeholder option will be
   * `null` instead of `undefined`.
   */
  nullable?: boolean;
  dataTestId?: string;
  options: Option<V>[];
}

export interface Option<T> {
  key?: string;
  label: string;
  value: T;
}

const EMPTY_VALUE = '';

export function FormSelect<T extends object, V>(props: SelectProps<T, V>) {
  const { field, fieldState } = useController(props);

  const selectedIndex = props.options.findIndex(option => option.value === field.value);
  const value = selectedIndex === -1 ? EMPTY_VALUE : selectedIndex;

  const error = fieldState.error?.message;

  const onChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const { value } = event.target;

    if (value === EMPTY_VALUE) {
      field.onChange(props.nullable ? null : undefined);
    } else {
      const index = Number(value);
      field.onChange(props.options[index]?.value);
    }
  };

  return (
    <Select
      {...field}
      label={props.label}
      error={error}
      value={value}
      onChange={onChange}
      data-testid={props.dataTestId}
    >
      <option value={EMPTY_VALUE}>{props.placeholder}</option>
      {props.options.map(({ key, label, value }, index) => (
        <option key={key ?? `${label}:${value}`} value={String(index)}>
          {String(label)}
        </option>
      ))}
    </Select>
  );
}
