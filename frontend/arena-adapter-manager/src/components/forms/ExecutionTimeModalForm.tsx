import {
  BodyShort,
  Button,
  DatePicker,
  ErrorMessage,
  Heading,
  HStack,
  Label,
  Modal,
  TextField,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useState } from "react";

interface ExecutionTimeModalFormProps {
  initDate?: Date;
  onSubmit: (date: Date) => void;
  onClose: () => void;
  displayData: Record<string, string>;
}
const headerText = "Execution time";

function getTime(date?: Date) {
  if (!date) {
    return "09:00";
  }
  const hours = date.getHours().toString().padStart(2, "0");
  const minutes = date.getMinutes().toString().padStart(2, "0");
  return `${hours}:${minutes}`;
}

export function ExecutionTimeModalForm({
  initDate,
  onSubmit,
  onClose,
  displayData,
}: ExecutionTimeModalFormProps) {
  const [time, setTime] = useState<string>(getTime(initDate));
  const [hasError, setHasError] = useState(false);

  const { datepickerProps, inputProps, selectedDay } = useDatepicker({
    fromDate: new Date("Jan 1 2023"),
    onDateChange: () => setHasError(false),
    defaultSelected: initDate,
  });

  function submitHandler(e: React.FormEvent) {
    e.preventDefault();
    if (selectedDay && time) {
      const [hour, minutes] = time.split(":").map((a) => parseInt(a));
      selectedDay.setHours(hour, minutes);
      onSubmit(selectedDay);
    } else {
      setHasError(true);
    }
  }
  return (
    <Modal width={500} aria-label={headerText} open={true} onClose={onClose}>
      <form onSubmit={submitHandler}>
        <Modal.Header>
          <Heading level="2" size="medium">
            {headerText}
          </Heading>
        </Modal.Header>
        <Modal.Body>
          <VStack gap="space-12">
            {Object.entries(displayData).map(([key, value]) => (
              <HStack key={key} gap="space-4">
                <Label>{key}:</Label>
                <BodyShort>{value}</BodyShort>
              </HStack>
            ))}
            <HStack gap="space-12">
              <DatePicker {...datepickerProps}>
                <DatePicker.Input
                  {...inputProps}
                  label="Velg dato"
                  error={hasError}
                  onChange={() => setHasError(false)}
                />
              </DatePicker>
              <TextField
                type="time"
                label="Tidspunkt"
                value={time}
                error={hasError}
                onChange={(e) => {
                  setHasError(false);
                  setTime(e.target.value);
                }}
              />
            </HStack>

            {hasError && <ErrorMessage showIcon>Dato er på feil format</ErrorMessage>}
          </VStack>
        </Modal.Body>
        <Modal.Footer>
          <HStack justify="space-between" width="100%" className="flex-row-reverse">
            <Button
              type="button"
              variant="secondary"
              data-color="danger"
              onClick={() => onSubmit(new Date())}
            >
              Run immediately 💥
            </Button>
            <Button type="submit">Queue...</Button>
          </HStack>
        </Modal.Footer>
      </form>
    </Modal>
  );
}
