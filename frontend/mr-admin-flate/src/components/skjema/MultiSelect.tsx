import React from "react";
import ReactSelect from "react-select";
import { SelectOption } from "./SokeSelect";

export interface MultiSelectProps {
  name: string;
  ref: React.Ref<any>;
  placeholder: string;
  options: SelectOption[];
  size?: "small" | "medium";
  value: SelectOption[];
  onChange: (e: any) => void;
  error: boolean;
}

const MultiSelect = React.forwardRef((props: MultiSelectProps) => {
  const { name, placeholder, options, onChange, value, ref, error } = props;

  const customStyles = (isError: boolean) => ({
    control: (provided: any, state: any) => ({
      ...provided,
      background: "#fff",
      borderColor: isError ? "#C30000" : "#0000008f",
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
      ref={ref}
      isMulti
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
