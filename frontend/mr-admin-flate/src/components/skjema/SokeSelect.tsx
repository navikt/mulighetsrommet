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
  defaultValue?: string;
  disabled?: boolean;
  onChange?: (a0: any) => void;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const SokeSelect = React.forwardRef((props: SelectProps, _) => {
  const {
    label,
    placeholder,
    options,
    defaultValue,
    disabled,
    onChange: providedOnChange,
    ...rest
  } = props;

  const customStyles = (isError: boolean) => ({
    control: (provided: any, state: any) => ({
      ...provided,
      background: disabled ? "#F1F1F1" : "#fff",
      borderColor: isError ? "#C30000" : "#0000008f",
      borderWidth: isError ? "2px" : "1px",
      height: "50px",
      boxShadow: state.isFocused ? null : null,
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
          <>
            <label className={style.label} htmlFor={name}>
              <b>{label}</b>
            </label>
            <ReactSelect
              placeholder={placeholder}
              isDisabled={!!disabled}
              className={style.sokeselect}
              ref={ref}
              noOptionsMessage={() => "Ingen funnet"}
              name={name}
              defaultInputValue={defaultValue}
              value={disabled ? null : options.find((c) => c.value === value)}
              onChange={(e) => {
                onChange(e?.value);
                providedOnChange?.(e?.value);
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
          </>
        )}
      />
    </>
  );
});

SokeSelect.displayName = "SokeSelect";

export { SokeSelect };
