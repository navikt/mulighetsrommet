import { UNSAFE_DatePicker, UNSAFE_useRangeDatepicker } from "@navikt/ds-react";
import style from "./Datovelger.module.scss";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useParams } from "react-router-dom";
import { formaterDato } from "../../utils/Utils";

interface DatoProps {
  startdato: any;
  sluttdato: any;
}

// Som dere ser er det mye som er testet ut her, men foreløpig ingen løsning på problemet på endre avtale/tiltaksgjennomføring.
// Denne filen blir kun testet i opprettelse/endring av avtaler

export const Dato = ({ startdato, sluttdato }: DatoProps) => {
  const { avtaleId } = useParams<{ avtaleId: string }>();
  const { data: avtale } = useAvtale(avtaleId);

  // const { field: startDato } = useController<inferredSchema, "startDato">({
  //   name: "startDato",
  // });
  // const { field: sluttDato } = useController<inferredSchema, "sluttDato">({
  //   name: "sluttDato",
  // });
  const offsetAntallAar = 3;

  const pastDate = () => {
    const newDate = new Date();
    const yearsAgo = newDate.setFullYear(
      newDate.getFullYear() - offsetAntallAar
    );
    return new Date(yearsAgo);
  };

  const futureDate = () => {
    const newDate = new Date();
    const yearsFromNow = newDate.setFullYear(
      newDate.getFullYear() + offsetAntallAar
    );
    return new Date(yearsFromNow);
  };

  const { datepickerProps, toInputProps, fromInputProps } =
    UNSAFE_useRangeDatepicker({
      allowTwoDigitYear: true,
      inputFormat: "dd.MM.yyyy",
      fromDate: pastDate(),
      toDate: futureDate(),
    });

  return (
    <div>
      <UNSAFE_DatePicker {...datepickerProps} dropdownCaption>
        <div className={style.datofelt}>
          <UNSAFE_DatePicker.Input
            {...fromInputProps}
            label="Startdato"
            // value={formaterDato(selectedRange?.from?.toString())}
            // value={formaterDato(startDato.value!!)}
            value={formaterDato(avtale!.startDato)}
          />
          <UNSAFE_DatePicker.Input
            {...toInputProps}
            label="Sluttdato"
            // value={formaterDato(selectedRange?.to?.toString())}
            // value={formaterDato(sluttDato.value!!)}
            value={formaterDato(avtale!.sluttDato)}
          />
        </div>
      </UNSAFE_DatePicker>
    </div>
  );
};
