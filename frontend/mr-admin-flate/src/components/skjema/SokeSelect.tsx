import React from "react";
import { Controller } from "react-hook-form";
import ReactSelect from "react-select";
import style from "./SokeSelect.module.scss";

export interface SelectOption {
  value?: string;
  label?: string;
}

export interface SelectProps {
  label: string;
  placeholder: string;
  options: SelectOption[];
  disabled?: boolean;
  onChange?: (a0: any) => void;
  onInputChange?: (a0: any) => void;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const SokeSelect = React.forwardRef((props: SelectProps, _) => {
  const {
    label,
    placeholder,
    options,
    disabled,
    onChange: providedOnChange,
    onInputChange: providedOnInputChange,
    ...rest
  } = props;

  const customStyles = (isError: boolean) => ({
    control: (provided: any, state: any) => ({
      ...provided,
      background: disabled ? "#F1F1F1" : "#fff",
      color: "red",
      borderColor: isError ? "#C30000" : "#0000008f",
      borderWidth: isError ? "2px" : "1px",
      height: "50px",
      boxShadow: state.isFocused ? null : null,
    }),
    clearIndicator: (provided: any) => ({
      ...provided,
      zIndex: "100",
    }),
    indicatorSeparator: () => ({
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
    singleValue: (provided: any) => ({
      ...provided,
      color: "black",
    }),
    menu: (provided: any) => ({
      ...provided,
      zIndex: "1000",
    }),
  });

  return (
    <>
      <Controller
        name={label}
        {...rest}
        render={({
          field: { onChange, value, name, ref },
          fieldState: { error },
        }) => (
          <div>
            <label className={style.label} htmlFor={name}>
              <b>{label}</b>
            </label>
            <ReactSelect
              placeholder={placeholder}
              isDisabled={!!disabled}
              isClearable
              ref={ref}
              noOptionsMessage={() => "Ingen funnet"}
              name={name}
              value={options.find((c) => c.value === value)}
              onChange={(e) => {
                onChange(e?.value);
                providedOnChange?.(e?.value);
              }}
              onInputChange={(e) => {
                providedOnInputChange?.(e);
              }}
              styles={customStyles(Boolean(error))}
              options={options}
              theme={(theme: any) => ({
                ...theme,
                colors: {
                  ...theme.colors,
                  primary25: "#cce1ff",
                  primary: "#0067c5",
                },
              })}
            />
            {error && (
              <div className={style.errormsg}>
                <b>• {error.message}</b>
              </div>
            )}
          </div>
        )}
      />
    </>
  );
});

SokeSelect.displayName = "SokeSelect";

export { SokeSelect };
