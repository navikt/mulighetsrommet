import { SelectOption } from "mulighetsrommet-frontend-common/components/SokeSelect";
import React, { ReactNode } from "react";
import ReactSelect from "react-select";

export interface MultiSelectProps {
  name: string;
  childRef: React.Ref<any>;
  placeholder: string;
  options: SelectOption[];
  noOptionsMessage?: ReactNode;
  size?: "small" | "medium";
  value: SelectOption[];
  readOnly?: boolean;
  onChange: (e: any) => void;
  error: boolean;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export const MultiSelect = React.forwardRef(function MultiSelect(props: MultiSelectProps, _) {
  const {
    name,
    placeholder,
    options,
    noOptionsMessage,
    onChange,
    value,
    childRef,
    error,
    readOnly,
    size,
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

  const allOptions =
    options.length > 1 ? [{ label: "Velg alle", value: "*" }, ...options] : options;

  return (
    <ReactSelect
      placeholder={placeholder}
      ref={childRef}
      isMulti
      isDisabled={!!readOnly}
      noOptionsMessage={() => (noOptionsMessage ? noOptionsMessage : "Ingen funnet")}
      name={name}
      value={value}
      onChange={(e) => {
        if (e.find((o) => o.value === "*")) {
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
});
