import { AnnetEnum } from "@/api/annetEnum";
import { useAvbrytTiltaksgjennomforing } from "@/api/tiltaksgjennomforing/useAvbrytTiltaksgjennomforing";
import { useTiltaksgjennomforingDeltakerSummary } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingDeltakerSummary";
import { Laster } from "@/components/laster/Laster";
import { AvbrytModalAarsaker } from "@/components/modal/AvbrytModalAarsaker";
import { AvbrytModalError } from "@/components/modal/AvbrytModalError";
import { VarselModal } from "@/components/modal/VarselModal";
import { Alert, BodyShort, Button, Radio } from "@navikt/ds-react";
import { AvbrytGjennomforingAarsak, Tiltaksgjennomforing } from "@mr/api-client";
import { RefObject, useState } from "react";
import { useNavigate } from "react-router-dom";
import z from "zod";

export const AvbrytGjennomforingModalSchema = z
  .object({
    aarsak: z.nativeEnum(
      { ...AvbrytGjennomforingAarsak, ...AnnetEnum },
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
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

interface State {
  aarsak?: AvbrytGjennomforingAarsak | AnnetEnum;
  customAarsak?: string;
  errors: { aarsakError?: string; customAarsakError?: string };
}

const initialState: State = {
  aarsak: undefined,
  customAarsak: undefined,
  errors: {},
};

export const AvbrytGjennomforingModal = ({ modalRef, tiltaksgjennomforing }: Props) => {
  const mutation = useAvbrytTiltaksgjennomforing();
  const navigate = useNavigate();
  const { data: deltakerSummary } = useTiltaksgjennomforingDeltakerSummary(tiltaksgjennomforing.id);
  const [state, setState] = useState<State>(initialState);

  const onClose = () => {
    setState(initialState);
    mutation.reset();
    modalRef.current?.close();
  };

  function onSuccessMutation() {
    setState(initialState);
    modalRef.current?.close();
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing?.id}`);
  }

  const handleAvbrytGjennomforing = () => {
    const parsed = AvbrytGjennomforingModalSchema.safeParse({
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

    if (parsed.success && tiltaksgjennomforing?.id && state?.aarsak) {
      if (state.aarsak === AnnetEnum.ANNET && state.customAarsak) {
        mutation.mutate(
          {
            id: tiltaksgjennomforing.id,
            aarsak: state.customAarsak,
          },
          { onSuccess: onSuccessMutation },
        );
      } else
        mutation.mutate(
          {
            id: tiltaksgjennomforing?.id,
            aarsak: state.aarsak,
          },
          { onSuccess: onSuccessMutation },
        );
    }
  };

  const aarsakToString = (aarsak: AvbrytGjennomforingAarsak): string => {
    switch (aarsak) {
      case AvbrytGjennomforingAarsak.AVBRUTT_I_ARENA:
        return "Avbrutt i Arena";
      case AvbrytGjennomforingAarsak.BUDSJETT_HENSYN:
        return "Budsjetthensyn";
      case AvbrytGjennomforingAarsak.ENDRING_HOS_ARRANGOR:
        return "Endring hos arrangør";
      case AvbrytGjennomforingAarsak.FEILREGISTRERING:
        return "Feilregistrering";
      case AvbrytGjennomforingAarsak.FOR_FAA_DELTAKERE:
        return "For få deltakere";
    }
  };

  return (
    <VarselModal
      modalRef={modalRef}
      handleClose={onClose}
      headingIconType="warning"
      headingText={
        mutation.isError
          ? `Kan ikke avbryte «${tiltaksgjennomforing?.navn}»`
          : `Ønsker du å avbryte «${tiltaksgjennomforing?.navn}»?`
      }
      body={
        <>
          {deltakerSummary && deltakerSummary.antallDeltakere > 0 && (
            <Alert variant="warning">
              {`Det finnes ${deltakerSummary.antallDeltakere} deltaker${deltakerSummary.antallDeltakere > 1 ? "e" : ""} på gjennomføringen. Ved å
           avbryte denne vil det føre til statusendring på alle deltakere som har en aktiv status.`}
            </Alert>
          )}
          <AvbrytModalAarsaker
            aarsak={state?.aarsak}
            customAarsak={state?.customAarsak}
            setAarsak={(aarsak) => {
              setState({
                ...state,
                aarsak: aarsak as AvbrytGjennomforingAarsak | AnnetEnum,
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
                {(Object.keys(AvbrytGjennomforingAarsak) as Array<AvbrytGjennomforingAarsak>)
                  .filter((a) => a !== AvbrytGjennomforingAarsak.AVBRUTT_I_ARENA)
                  .map((a) => (
                    <Radio key={`${a}`} value={a}>
                      {aarsakToString(a)}
                    </Radio>
                  ))}
              </>
            }
          />
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
      secondaryButton
      primaryButton={
        <Button
          variant="danger"
          onClick={handleAvbrytGjennomforing}
          disabled={mutation.isPending}
          style={{ minWidth: "20rem" }}
        >
          {mutation.isPending ? <Laster /> : "Ja, jeg vil avbryte gjennomføringen"}
        </Button>
      }
    />
  );
};
