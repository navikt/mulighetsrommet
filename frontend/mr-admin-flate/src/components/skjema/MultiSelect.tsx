import { SelectOption } from "mulighetsrommet-frontend-common/components/SokeSelect";
import React, { ReactNode, Ref } from "react";
import ReactSelect from "react-select";
import Select from "react-select/base";

export interface MultiSelectProps<T> {
  name: string;
  placeholder: string;
  options: SelectOption<T>[];
  noOptionsMessage?: ReactNode;
  size?: "small" | "medium";
  value: SelectOption<T>[];
  readOnly?: boolean;
  onChange: (e: any) => void;
  onInputChange?: (value: string) => void;
  error: boolean;
  velgAlle?: boolean;
}

export const MultiSelect = <T,>(
  props: MultiSelectProps<T | null> & { childRef?: Ref<Select<SelectOption<T | null>>> },
) => {
  const {
    name,
    placeholder,
    options,
    noOptionsMessage,
    onChange,
    onInputChange,
    value,
    childRef,
    error,
    readOnly,
    size,
    velgAlle = false,
  } = props;

  const customStyles = (isError: boolean) => ({
    control: (provided: any, state: any) => ({
      ...provided,
      background: readOnly ? "#F1F1F1" : "#fff",
      borderColor: isError ? "#C30000" : readOnly ? "#0000001A" : "#0000008f",
      borderWidth: isError ? "2px" : "1px",
      boxShadow: state.isFocused ? "0 0 0 3px rgba(0, 52, 125, 1)" : null,
    }),
    multiValue: (provided: any) => ({
      ...provided,
      backgroundColor: "#005b82",
      borderRadius: "15px",
      color: "white",
    }),
    multiValueLabel: (provided: any) => ({
      ...provided,
      color: "white",
    }),
    multiValueRemove: (provided: any) => ({
      ...provided,
      color: "#cce1ff",
      ":hover": {
        backgroundColor: "#3380a5",
        borderRadius: "15px",
        color: "white",
      },
    }),
    indicatorSeparator: (state: any) => ({
      ...state,
      display: "none",
    }),
    dropdownIndicator: (provided: any) => ({
      ...provided,
      color: "black",
    }),
    placeholder: (provided: any) => ({
      ...provided,
      color: "#0000008f",
    }),
  });

  const allOptions = (velgAlle && options.length > 1)
    ? [{ label: "Velg alle", value: null }, ...options]
    : options;

  return (
    <ReactSelect
      placeholder={placeholder}
      ref={childRef}
      isMulti
      isDisabled={!!readOnly}
      noOptionsMessage={() => (noOptionsMessage ? noOptionsMessage : "Ingen funnet")}
      name={name}
      value={value}
      onInputChange={onInputChange}
      onChange={(e) => {
        if (velgAlle && e && "find" in e && e.find((o) => o.value === null)) {
          if (value.length === options.length) {
            onChange([]);
          } else {
            onChange(options);
          }
        } else {
          onChange(e);
        }
      }}
      styles={customStyles(Boolean(error))}
      options={allOptions}
      theme={(theme: any) => ({
        ...theme,
        spacing: {
          ...theme.spacing,
          controlHeight: size === "small" ? 32 : 48,
          baseUnit: 2,
        },
        colors: {
          ...theme.colors,
          primary25: "#cce1ff",
          primary: "#0067c5",
          danger: "black",
          dangerLight: "#0000004f",
          neutral10: "#cce1ff",
        },
      })}
    />
  );
};
