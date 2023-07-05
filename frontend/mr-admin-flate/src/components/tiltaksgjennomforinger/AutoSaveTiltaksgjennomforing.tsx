import debounce from "debounce";
import {
  Avtale,
  Tiltaksgjennomforing,
  Utkast,
} from "mulighetsrommet-api-client";
import { memo, useCallback, useEffect } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { toast } from "react-toastify";
import useDeepCompareEffect from "use-deep-compare-effect";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { useUtkast } from "../../api/utkast/useUtkast";
import { inferredTiltaksgjennomforingSchema } from "./OpprettTiltaksgjennomforingContainer";

type Props = {
  defaultValues: any;
  utkastId: string;
  avtale: Avtale;
};

type UtkastData = Pick<
  Tiltaksgjennomforing,
  | "navn"
  | "antallPlasser"
  | "startDato"
  | "sluttDato"
  | "navEnheter"
  | "stengtFra"
  | "stengtTil"
  | "arrangorOrganisasjonsnummer"
  | "kontaktpersoner"
  | "estimertVentetid"
  | "lokasjonArrangor"
> & {
  tiltakstypeId: string;
  avtaleId: string;
  arrangorKontaktpersonId?: string | null;
  id: string;
};

// TODO Vurdere å ta ting data som props og gjenbruke autosave på tvers av gjennomføring og avtale
export const AutoSaveTiltaksgjennomforing = memo(
  ({ defaultValues, utkastId, avtale }: Props) => {
    if (!utkastId) throw new Error("Ingen utkastId tilgjengelig");

    const methods = useFormContext<inferredTiltaksgjennomforingSchema>();
    const { data } = useHentAnsatt();
    const mutation = useMutateUtkast();

    const { data: lagretUtkast } = useUtkast(utkastId);

    const debouncedSave = useCallback(
      debounce(() => {
        const values = methods.getValues();

        const utkastData: UtkastData = {
          navn: values?.navn,
          antallPlasser: values?.antallPlasser,
          startDato: values?.startOgSluttDato?.startDato?.toDateString(),
          sluttDato: values?.startOgSluttDato?.sluttDato?.toDateString(),
          navEnheter: values?.navEnheter?.map((enhetsnummer) => ({
            navn: "",
            enhetsnummer,
          })),
          stengtFra: values?.midlertidigStengt?.erMidlertidigStengt
            ? values?.midlertidigStengt?.stengtFra?.toString()
            : undefined,
          stengtTil: values?.midlertidigStengt?.erMidlertidigStengt
            ? values?.midlertidigStengt?.stengtTil?.toString()
            : undefined,
          tiltakstypeId: avtale?.tiltakstype.id,
          avtaleId: avtale?.id,
          arrangorKontaktpersonId: values?.arrangorKontaktpersonId,
          arrangorOrganisasjonsnummer:
            values.tiltaksArrangorUnderenhetOrganisasjonsnummer,
          kontaktpersoner:
            values?.kontaktpersoner?.map((kp) => ({ ...kp })) || [],
          id: utkastId,
          lokasjonArrangor: values?.lokasjonArrangor,
          estimertVentetid: values?.estimertVentetid,
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
          avtaleId: avtale.id,
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
