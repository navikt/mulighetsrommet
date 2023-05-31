import React from "react";
import ReactSelect from "react-select";
import { SelectOption } from "./SokeSelect";

export interface MultiSelectProps {
  name: string;
  childRef: React.Ref<any>;
  placeholder: string;
  options: SelectOption[];
  size?: "small" | "medium";
  value: SelectOption[];
  readOnly?: boolean;
  onChange: (e: any) => void;
  error: boolean;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const MultiSelect = React.forwardRef((props: MultiSelectProps, _) => {
  const {
    name,
    placeholder,
    options,
    onChange,
    value,
    childRef,
    error,
    readOnly,
  } = props;

  const customStyles = (isError: boolean) => ({
    control: (provided: any, state: any) => ({
      ...provided,
      background: readOnly ? "#F1F1F1" : "#fff",
      borderColor: isError ? "#C30000" : (readOnly ? "#0000001A" : "#0000008f"),
      borderWidth: isError ? "2px" : "1px",
      minHeight: "48px",
      boxShadow: state.isFocused ? null : null,
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
  return (
    <ReactSelect
      placeholder={placeholder}
      ref={childRef}
      isMulti
      isDisabled={!!readOnly}
      noOptionsMessage={() => "Ingen funnet"}
      name={name}
      value={value}
      onChange={onChange}
      styles={customStyles(Boolean(error))}
      options={options}
      theme={(theme: any) => ({
        ...theme,
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

MultiSelect.displayName = "MultiSelect";

export { MultiSelect };
