import styles from "../notater/Notater.module.scss";
import { Button, Checkbox, ErrorMessage, Heading, Textarea } from "@navikt/ds-react";
import { useState } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { TiltaksgjennomforingNotatRequest } from "mulighetsrommet-api-client";
import { v4 as uuidv4 } from "uuid";
import { zodResolver } from "@hookform/resolvers/zod";
import invariant from "tiny-invariant";
import { Laster } from "../laster/Laster";
import { inferredNotatSchema, NotatSchema } from "../notater/NotatSchema";
import Notatliste from "../notater/Notatliste";
import { useTiltaksgjennomforingsnotater } from "../../api/notater/gjennomforingsnotat/useTiltaksgjennomforingsnotater";
import { useMineTiltaksgjennomforingsnotater } from "../../api/notater/gjennomforingsnotat/useMineTiltaksgjennomforingsnotater";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { usePutTiltaksgjennomforingsnotat } from "../../api/notater/gjennomforingsnotat/usePutTiltaksgjennomforingsnotat";
import { useDeleteTiltaksgjennomforingsnotat } from "../../api/notater/gjennomforingsnotat/useDeleteTiltaksgjennomforingsnotat";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";

export default function NotaterTiltaksgjennomforingerPage() {
  const { data: notater = [] } = useTiltaksgjennomforingsnotater();
  const { data: mineNotater = [] } = useMineTiltaksgjennomforingsnotater();
  const { data: tiltaksgjennomforingsData } = useTiltaksgjennomforingById();

  const putTiltaksgjennomforingsnotat = usePutTiltaksgjennomforingsnotat();
  const deleteTiltaksgjennomforingsnotat = useDeleteTiltaksgjennomforingsnotat();

  const [visMineNotater, setVisMineNotater] = useState(false);
  const liste = visMineNotater ? mineNotater : notater;

  const form = useForm<inferredNotatSchema>({
    resolver: zodResolver(NotatSchema),
    defaultValues: {
      innhold: "",
    },
  });

  const {
    handleSubmit,
    formState: { errors },
    register,
    reset,
    watch,
  } = form;

  const postData: SubmitHandler<inferredNotatSchema> = async (data): Promise<void> => {
    const { innhold } = data;
    invariant(tiltaksgjennomforingsData, "Klarte ikke hente tiltaksgjennomføring.");

    const requestBody: TiltaksgjennomforingNotatRequest = {
      id: uuidv4(),
      tiltaksgjennomforingId: tiltaksgjennomforingsData.id,
      innhold,
    };
    putTiltaksgjennomforingsnotat.mutate(requestBody, {
      onSuccess: () => reset(),
    });
  };

  return (
    <div className={styles.notater}>
      <HarSkrivetilgang ressurs="Tiltaksgjennomføring">
        <FormProvider {...form}>
          <form onSubmit={handleSubmit(postData)}>
            <div className={styles.notater_opprett}>
              <Textarea
                label={"Innhold for notat"}
                hideLabel
                className={styles.notater_input}
                error={errors.innhold?.message}
                minRows={15}
                maxRows={25}
                resize
                maxLength={500}
                {...register("innhold")}
                value={watch("innhold")}
              />
              {putTiltaksgjennomforingsnotat.isError ? (
                <ErrorMessage>Det skjedde en feil. Notatet ble ikke lagret.</ErrorMessage>
              ) : null}
              <span className={styles.notater_knapp}>
                <Button type="submit" disabled={putTiltaksgjennomforingsnotat.isPending}>
                  {putTiltaksgjennomforingsnotat.isPending ? <Laster /> : "Legg til notat"}
                </Button>
              </span>
            </div>
          </form>
        </FormProvider>
      </HarSkrivetilgang>

      <div className={styles.notater_notatvegg}>
        <Heading size="medium" level="3" className={styles.notater_heading}>
          Notater
        </Heading>

        <div className={styles.notater_andrerad}>
          <Checkbox onChange={() => setVisMineNotater(!visMineNotater)}>
            Vis kun mine notater
          </Checkbox>
        </div>

        <Notatliste
          notater={liste}
          visMineNotater={visMineNotater}
          mutation={deleteTiltaksgjennomforingsnotat}
        />
      </div>
    </div>
  );
}
