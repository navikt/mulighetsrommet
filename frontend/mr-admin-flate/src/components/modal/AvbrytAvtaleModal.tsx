import { AnnetEnum } from "@/api/annetEnum";
import { useAvbrytAvtale } from "@/api/avtaler/useAvbrytAvtale";
import { useAktiveTiltaksgjennomforingerByAvtaleId } from "@/api/tiltaksgjennomforing/useAktiveTiltaksgjennomforingerByAvtaleId";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { Laster } from "@/components/laster/Laster";
import { AvbrytModalAarsaker } from "@/components/modal/AvbrytModalAarsaker";
import { AvbrytModalError } from "@/components/modal/AvbrytModalError";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { avbrytAvtaleAarsakToString } from "@/utils/Utils";
import { BodyShort, Button, Radio } from "@navikt/ds-react";
import { AvbrytAvtaleAarsak, Avtale } from "@mr/api-client";
import { RefObject, useState } from "react";
import { useNavigate } from "react-router-dom";
import z from "zod";
import style from "./AvbrytGjennomforingAvtaleModal.module.scss";

export const AvbrytAvtaleModalSchema = z
  .object({
    aarsak: z.nativeEnum(
      { ...AvbrytAvtaleAarsak, ...AnnetEnum },
      { required_error: "Mangler årsak" },
    ),
    customAarsak: z.string().max(100, "Beskrivelse kan ikke inneholde mer enn 100 tegn").optional(),
  })
  .superRefine((data, ctx) => {
    if (data.aarsak === AnnetEnum.ANNET && !data.customAarsak) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["customAarsak"],
        message: "Beskrivelse er obligatorisk når “Annet” er valgt som årsak",
      });
    }
  });

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  avtale: Avtale;
}

interface State {
  aarsak?: AvbrytAvtaleAarsak | AnnetEnum;
  customAarsak?: string;
  errors: { aarsakError?: string; customAarsakError?: string };
}

const initialState: State = {
  aarsak: undefined,
  customAarsak: undefined,
  errors: {},
};

export function AvbrytAvtaleModal({ modalRef, avtale }: Props) {
  const mutation = useAvbrytAvtale();
  const navigate = useNavigate();
  const { data: tiltaksgjennomforingerMedAvtaleId } = useAktiveTiltaksgjennomforingerByAvtaleId(
    avtale.id,
  );
  const [state, setState] = useState<State>(initialState);

  const avtalenHarGjennomforinger =
    tiltaksgjennomforingerMedAvtaleId && tiltaksgjennomforingerMedAvtaleId.data.length > 0;

  const onClose = () => {
    setState(initialState);
    mutation.reset();
    modalRef.current?.close();
  };

  function navigateOnSuccess() {
    setState(initialState);
    modalRef.current?.close();
    navigate(`/avtaler/${avtale.id}`);
  }

  const handleAvbrytAvtale = () => {
    const parsed = AvbrytAvtaleModalSchema.safeParse({
      aarsak: state?.aarsak,
      customAarsak: state?.customAarsak,
    });
    if (!parsed.success) {
      const aarsakErrors = parsed.error.format();
      setState({
        ...state,
        errors: {
          aarsakError: aarsakErrors.aarsak?._errors.join("\n"),
          customAarsakError: aarsakErrors.customAarsak?._errors.join("\n"),
        },
      });
    }

    if (parsed.success && avtale?.id && state?.aarsak) {
      if (state.aarsak === AnnetEnum.ANNET && state.customAarsak) {
        mutation.mutate(
          {
            id: avtale.id,
            aarsak: state.customAarsak,
          },
          { onSuccess: navigateOnSuccess },
        );
      } else
        mutation.mutate(
          {
            id: avtale?.id,
            aarsak: state.aarsak,
          },
          { onSuccess: navigateOnSuccess },
        );
    }
  };

  function pluralGjennomforingTekst(antall: number, tekst: string) {
    return tiltaksgjennomforingerMedAvtaleId &&
      tiltaksgjennomforingerMedAvtaleId.data.length > antall
      ? tekst
      : "";
  }

  return (
    <VarselModal
      modalRef={modalRef}
      handleClose={onClose}
      headingIconType="warning"
      headingText={
        mutation.isError || avtalenHarGjennomforinger
          ? `Kan ikke avbryte «${avtale?.navn}»`
          : `Ønsker du å avbryte «${avtale?.navn}»?`
      }
      body={
        <>
          {avtalenHarGjennomforinger ? (
            <BodyShort>
              {`Avtaler med aktive gjennomføringer kan ikke avbrytes. Det er
                ${tiltaksgjennomforingerMedAvtaleId.data.length}
                aktiv${pluralGjennomforingTekst(1, "e")}
                gjennomføring${pluralGjennomforingTekst(1, "er")}
                under denne avtalen. Vurder om du vil avbryte
                gjennomføringen${pluralGjennomforingTekst(0, "e")}. `}
            </BodyShort>
          ) : (
            <AvbrytModalAarsaker
              aarsak={state?.aarsak}
              customAarsak={state?.customAarsak}
              setAarsak={(aarsak) => {
                setState({
                  ...state,
                  aarsak: aarsak as AvbrytAvtaleAarsak | AnnetEnum,
                  errors: { ...state.errors, aarsakError: undefined },
                });
              }}
              setCustomAarsak={(customAarsak) => {
                setState({
                  ...state,
                  customAarsak,
                  errors: { ...state.errors, customAarsakError: undefined },
                });
              }}
              mutation={mutation}
              aarsakError={state?.errors.aarsakError}
              customAarsakError={state?.errors.customAarsakError}
              radioknapp={
                <>
                  {(Object.keys(AvbrytAvtaleAarsak) as Array<AvbrytAvtaleAarsak>)
                    .filter((a) => a !== AvbrytAvtaleAarsak.AVBRUTT_I_ARENA)
                    .map((a) => (
                      <Radio key={`${a}`} value={a}>
                        {avbrytAvtaleAarsakToString(a)}
                      </Radio>
                    ))}
                </>
              }
            />
          )}

          {mutation?.isError && (
            <BodyShort>
              <AvbrytModalError
                aarsak={state.aarsak}
                customAarsak={state?.customAarsak}
                mutation={mutation}
              />
            </BodyShort>
          )}
        </>
      }
      secondaryButton={!avtalenHarGjennomforinger}
      primaryButton={
        avtalenHarGjennomforinger ? (
          <Button onClick={onClose}>Ok</Button>
        ) : (
          <HarSkrivetilgang ressurs="Avtale">
            <Button
              variant="danger"
              onClick={handleAvbrytAvtale}
              disabled={mutation.isPending}
              style={{ minWidth: "15rem" }}
            >
              {mutation.isPending ? <Laster /> : "Ja, jeg vil avbryte avtalen"}
            </Button>
          </HarSkrivetilgang>
        )
      }
      footerClassName={avtalenHarGjennomforinger ? style.footer_ok : ""}
    />
  );
}
