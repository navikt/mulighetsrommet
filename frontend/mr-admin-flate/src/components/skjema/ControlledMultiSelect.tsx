import { Controller } from "react-hook-form";
import { SelectOption } from "./SokeSelect";
import { MultiSelect } from "./MultiSelect";

export interface MultiSelectProps {
  label: string;
  placeholder: string;
  options: SelectOption[];
  size?: "small" | "medium",
}

export const ControlledMultiSelect = (props: MultiSelectProps) => {
  const {
    label,
    placeholder,
    options,
    ...rest
  } = props;

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
            <MultiSelect
              error={Boolean(error)}
              placeholder={placeholder}
              ref={ref}
              name={name}
              value={
                options.filter((c) => value?.includes(c.value))
              }
              onChange={(e) => {
                onChange(e.map((option: SelectOption) => option.value))
              }}
              options={options}
            />
          </>
        )}
      />
    </div>
  );
}
