import styles from "./Notater.module.scss";
import {
  Button,
  Checkbox,
  ErrorMessage,
  Heading,
  Textarea,
} from "@navikt/ds-react";
import Notatliste from "./Notatliste";
import { useAvtalenotater } from "../../api/avtaler/avtalenotat/useAvtalenotater";
import { useMineAvtalenotater } from "../../api/avtaler/avtalenotat/useMineAvtalenotater";
import { useState } from "react";
import { usePutAvtalenotat } from "../../api/avtaler/avtalenotat/usePutAvtalenotat";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { inferredAvtalenotatSchema, NotatSchema } from "./NotatSchema";
import { AvtaleNotatRequest } from "mulighetsrommet-api-client";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { v4 as uuidv4 } from "uuid";
import { zodResolver } from "@hookform/resolvers/zod";
import invariant from "tiny-invariant";
import { Laster } from "../laster/Laster";

export default function NotaterPage() {
  const { data: notater = [] } = useAvtalenotater();
  const { data: mineNotater = [] } = useMineAvtalenotater();
  const { data: avtaleData } = useAvtale();

  const mutation = usePutAvtalenotat();
  const [visMineNotater, setVisMineNotater] = useState(false);
  const liste = visMineNotater ? mineNotater : notater;

  const form = useForm<inferredAvtalenotatSchema>({
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

  const postData: SubmitHandler<inferredAvtalenotatSchema> = async (
    data,
  ): Promise<void> => {
    const { innhold } = data;
    invariant(avtaleData, "Klarte ikke hente avtale.");

    const requestBody: AvtaleNotatRequest = {
      id: uuidv4(),
      avtaleId: avtaleData.id,
      innhold,
    };

    mutation.mutate(requestBody, { onSuccess: () => reset() });
  };

  return (
    <div className={styles.notater}>
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
            {mutation.isError ? (
              <ErrorMessage>
                Det skjedde en feil. Notatet ble ikke lagret.
              </ErrorMessage>
            ) : null}
            <span className={styles.notater_knapp}>
              <Button type="submit" disabled={mutation.isLoading}>
                {mutation.isLoading ? <Laster /> : "Legg til notat"}
              </Button>
            </span>
          </div>
        </form>
      </FormProvider>

      <div className={styles.notater_notatvegg}>
        <Heading size="medium" level="3" className={styles.notater_heading}>
          Notater
        </Heading>

        <div className={styles.notater_andrerad}>
          <Checkbox onChange={() => setVisMineNotater(!visMineNotater)}>
            Vis kun mine notater
          </Checkbox>
        </div>

        <Notatliste notater={liste} visMineNotater={visMineNotater} />
      </div>
    </div>
  );
}
