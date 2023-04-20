import { Controller } from "react-hook-form";
import { SelectOption } from "./SokeSelect";
import { MultiSelect } from "./MultiSelect";
import React from "react";

export interface MultiSelectProps {
  label: string;
  placeholder: string;
  options: SelectOption[];
  size?: "small" | "medium";
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const ControlledMultiSelect = React.forwardRef((props: MultiSelectProps, _) => {
  const { label, placeholder, options, ...rest } = props;

  return (
    <div>
      <Controller
        name={label}
        {...rest}
        render={({
          field: { onChange, value, name, ref },
          fieldState: { error },
        }) => (
          <>
            <label
              style={{ marginBottom: "8px", display: "inline-block" }}
              htmlFor={name}
            >
              <b>{label}</b>
            </label>
            <MultiSelect
              error={Boolean(error)}
              placeholder={placeholder}
              ref={ref}
              name={name}
              value={options.filter((c) => value?.includes(c.value))}
              onChange={(e) => {
                onChange(e.map((option: SelectOption) => option.value));
              }}
              options={options}
            />
            {error && (
              <div style={{ marginTop: "8px", color: "#C30000" }}>
                <b>• {error.message}</b>
              </div>
            )}
          </>
        )}
      />
    </div>
  );
});

ControlledMultiSelect.displayName = "ControlledMultiSelect";

export { ControlledMultiSelect };
