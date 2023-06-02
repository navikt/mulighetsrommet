import classnames from "classnames";
import React from "react";
import { Controller } from "react-hook-form";
import ReactSelect from "react-select";
import styles from "./SokeSelect.module.scss";

export interface SelectOption {
  value?: string;
  label?: string;
}

export interface SelectProps {
  label: string;
  hideLabel?: boolean;
  placeholder: string;
  options: SelectOption[];
  readOnly?: boolean;
  onChange?: (a0: any) => void;
  onInputChange?: (a0: any) => void;
  isClearable?: boolean;
  className?: string;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const SokeSelect = React.forwardRef((props: SelectProps, _) => {
  const {
    label,
    hideLabel = false,
    placeholder,
    options,
    readOnly,
    onChange: providedOnChange,
    onInputChange: providedOnInputChange,
    isClearable = true,
    className,
    ...rest
  } = props;

  const customStyles = (isError: boolean) => ({
    control: (provided: any, state: any) => ({
      ...provided,
      background: readOnly ? "#f1f1f1" : "#fff",
      borderColor: isError ? "#C30000" : readOnly ? "#0000001A" : "#0000008f",
      borderWidth: isError ? "2px" : "1px",
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
            <label
              className={classnames(styles.label, {
                "navds-sr-only": hideLabel,
              })}
              htmlFor={name}
            >
              <b>{label}</b>
            </label>

            <ReactSelect
              placeholder={placeholder}
              isDisabled={!!readOnly}
              isClearable={isClearable}
              ref={ref}
              inputId={name}
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
              className={className}
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
              <div className={styles.errormsg}>
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
