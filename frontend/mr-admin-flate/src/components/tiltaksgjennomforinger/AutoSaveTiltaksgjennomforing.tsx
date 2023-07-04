import debounce from "debounce";
import {
  TiltaksgjennomforingRequest,
  Utkast,
} from "mulighetsrommet-api-client";
import { memo, useCallback, useEffect } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { toast } from "react-toastify";
import useDeepCompareEffect from "use-deep-compare-effect";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { useUtkast } from "../../api/utkast/useUtkast";

type Props = {
  defaultValues: any;
  utkastId: string;
  avtaleId: string;
};
// TODO Vurdere å ta ting data som props og gjenbruke autosave på tvers av gjennomføring og avtale
export const AutoSaveTiltaksgjennomforing = memo(
  ({ defaultValues, utkastId, avtaleId }: Props) => {
    if (!utkastId) throw new Error("Ingen utkastId tilgjengelig");

    const methods = useFormContext();
    const { data } = useHentAnsatt();
    const mutation = useMutateUtkast();

    const { data: lagretUtkast } = useUtkast(utkastId);

    const debouncedSave = useCallback(
      debounce(() => {
        const values = methods.getValues() as any; // TODO Fiks bruk av any

        // TODO Rydd i dette rotet her
        const utkastData: TiltaksgjennomforingRequest = {
          ...values,
          startDato: values?.startOgSluttDato?.startDato,
          sluttDato: values?.startOgSluttDato?.sluttDato,
          navEnheter: values?.navEnheter?.map((enhetsnummer: string) => ({
            navn: "",
            enhetsnummer,
          })),
          stengtFra: values?.midlertidigStengt?.erMidlertidigStengt
            ? values?.midlertidigStengt?.stengtFra
            : undefined,
          stengtTil: values?.midlertidigStengt?.erMidlertidigStengt
            ? values?.midlertidigStengt?.stengtTil
            : undefined,
          id: utkastId,
        };

        if (!values.navn) {
          toast.info(
            "For å lagre utkast må du skrive et navn for gjennomføringen",
            {
              autoClose: 10000,
            }
          );
          return;
        }

        mutation.mutate({
          id: utkastId,
          utkastData,
          type: Utkast.type.TILTAKSGJENNOMFORING,
          opprettetAv: lagretUtkast?.opprettetAv || data?.navIdent,
          avtaleId,
        });
      }, 1000),
      []
    );

    useEffect(() => {
      if (mutation.isSuccess) {
        toast.success("Utkast lagret", {
          toastId: `success-${utkastId}`, // For å hindre duplikate meldinger
          hideProgressBar: true,
          autoClose: 2000,
        });
      }

      if (mutation.isError) {
        toast.error("Klarte ikke lagre utkast", {
          toastId: `error-${utkastId}`, // For å hindre duplikate meldinger
          hideProgressBar: true,
        });
      }
    }, [mutation]);

    const watchedData = useWatch({
      control: methods.control,
      defaultValue: defaultValues,
    });

    useDeepCompareEffect(() => {
      if (methods.formState.isDirty) {
        debouncedSave();
      }
    }, [watchedData]);

    return null;
  }
);

AutoSaveTiltaksgjennomforing.displayName = "AutoSaveTiltaksgjennomforing";
