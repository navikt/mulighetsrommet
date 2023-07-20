import styles from "./Notater.module.scss";
import { Button, Checkbox, Heading, Loader, Textarea } from "@navikt/ds-react";
import Notatliste from "./Notatliste";
import { useAvtalenotater } from "../../api/avtaler/avtalenotat/useAvtalenotater";
import { useMineAvtalenotater } from "../../api/avtaler/avtalenotat/useMineAvtalenotater";
import { useEffect, useState } from "react";
import { usePutAvtalenotat } from "../../api/avtaler/avtalenotat/usePutAvtalenotat";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { inferredAvtalenotatSchema, NotatSchema } from "./NotatSchema";
import { AvtaleNotatRequest } from "mulighetsrommet-api-client";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { v4 as uuidv4 } from "uuid";
import { zodResolver } from "@hookform/resolvers/zod";

export default function NotaterPage() {
  const { data: notatkortListe } = useAvtalenotater();
  const { data: avtaleData } = useAvtale();
  const { data: mineNotaterListe } = useMineAvtalenotater();

  const mutation = usePutAvtalenotat();
  const [mineNotater, setMineNotater] = useState(false);
  const liste = mineNotater ? mineNotaterListe : notatkortListe;

  const form = useForm<inferredAvtalenotatSchema>({
    resolver: zodResolver(NotatSchema),
    defaultValues: {
      innhold: "",
    },
  });

  const {
    handleSubmit,
    formState: { errors, isSubmitSuccessful },
    register,
    reset,
  } = form;

  const postData: SubmitHandler<inferredAvtalenotatSchema> = async (
    data,
  ): Promise<void> => {
    const { innhold } = data;

    const requestBody: AvtaleNotatRequest = {
      id: uuidv4(),
      avtaleId: avtaleData!.id,
      innhold,
    };

    mutation.mutate(requestBody);
  };

  useEffect(() => {
    if (isSubmitSuccessful) {
      reset({ innhold: "" });
    }
  }, [isSubmitSuccessful, reset]);

  return (
    <div className={styles.notater}>
      <FormProvider {...form}>
        <form onSubmit={handleSubmit(postData)}>
          <div className={styles.notater_opprett}>
            <Textarea
              label={""}
              hideLabel
              className={styles.notater_input}
              {...register("innhold")}
              error={errors.innhold?.message}
            />
            <span className={styles.notater_knapp}>
              <Button type="submit">
                {mutation.isLoading ? <Loader /> : "Legg til notat"}
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
          <Checkbox onChange={() => setMineNotater(!mineNotater)}>
            Vis kun mine notater
          </Checkbox>
        </div>

        <Notatliste notatListe={liste!} />
      </div>
    </div>
  );
}
