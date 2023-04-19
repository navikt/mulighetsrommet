import ReactSelect from "react-select";
import { SelectOption } from "./SokeSelect";

export interface MultiSelectProps {
  name: string;
  ref: React.Ref<any>;
  placeholder: string;
  options: SelectOption[];
  size?: "small" | "medium",
  value: SelectOption[];
  onChange: (e: any) => void,
  error: boolean;
}

export const MultiSelect = (props: MultiSelectProps) => {
  const {
    name,
    placeholder,
    options,
    onChange,
    value,
    ref,
    error,
  } = props;

  const customStyles = (isError: boolean) => ({
    control: (provided: any, state: any) => ({
      ...provided,
      background: '#fff',
      borderColor: isError ? "#C30000" : "#0000008f",
      borderWidth: isError ? "2px" : "1px",
      minHeight: "48px",
      boxShadow: state.isFocused ? null : null,
    }),
    indicatorSeparator: (state: any) => ({
      display: 'none',
    }),
  });

  return (
    <ReactSelect
      placeholder={placeholder}
      ref={ref}
      isMulti
      name={name}
      value={value}
      onChange={onChange}
      styles={customStyles(Boolean(error))}
      options={options}
      theme={(theme: any) => ({
        ...theme,
        colors: {
          ...theme.colors,
          primary25: '#cce1ff',
          primary: '#0067c5',
          danger: "black",
          dangerLight: "#0000004f",
          neutral10: "#cce1ff",
        },
      })}
    />
  );
}
