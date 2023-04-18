import ReactSelect from "react-select";
import { Controller } from "react-hook-form";

export interface SelectOption {
  value?: string;
  label?: string;
}

export interface SelectProps {
  label: string;
  placeholder: string;
  options: SelectOption[];
}

export const SokeSelect = (props: SelectProps) => {
  const {
    label,
    placeholder,
    options,
    ...rest
  } = props;

  const customStyles = (isError: boolean) => ({
    control: (provided: any, state: any) => ({
      ...provided,
      background: '#fff',
      borderColor: isError ? "#C30000" : "#0000008f",
      borderWidth: isError ? "2px" : "1px",
      height: '50px',
      boxShadow: state.isFocused ? null : null,
    }),
    indicatorSeparator: (state: any) => ({
      display: 'none',
    }),
  });

  return (
    <div>
      <div style={{ marginBottom: "8px" }}>
        <b>{label}</b>
      </div>
      <Controller
        name={label}
        {...rest}
        render={({
            field: { onChange, value, name, ref },
            fieldState: { error },
        }) => (
            <>
          <ReactSelect
            placeholder={placeholder}
            ref={ref}
            name={name}
            value={options.find((c) => c.value === value) || { label: placeholder, value: ""}}
            onChange={(e) => { onChange(e?.value) }}
            styles={customStyles(Boolean(error))}
            options={options}
            theme={(theme: any) => ({
                ...theme,
                colors: {
                  ...theme.colors,
                  primary25: '#cce1ff',
                  primary: '#0067c5',
                },
              })}
          />
          {error && <div style={{ marginTop: "8px", color: "#C30000" }}><b>• {error.message}</b></div>}
          </>
        )}
      />
    </div>
  );
}
