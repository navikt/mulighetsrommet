import { ReactNode, useState } from "react";
import { Section } from "../components/Section";
import { ApiBase, MrApiTask, runTask } from "../core/api";
import { Button, HStack } from "@navikt/ds-react";

interface Props {
  base: ApiBase;
  task: MrApiTask;
  form?: (props: { onSubmit: (data: any) => void; loading: boolean }) => ReactNode;
  children?: ReactNode;
}

export function RunTask(props: Props) {
  const [loading, setLoading] = useState(false);

  const executeTask = (input?: object) => {
    setLoading(true);

    return runTask(props.base, props.task, input).finally(() => {
      setLoading(false);
    });
  };

  return (
    <Section headerText={props.task} loadingText={"Laster"} isLoading={loading}>
      {props.children}
      {props.form ? (
        props.form({ onSubmit: executeTask, loading })
      ) : (
        <HStack align="start">
          <Button disabled={loading} onClick={() => executeTask()}>
            Run task 💥
          </Button>
        </HStack>
      )}
    </Section>
  );
}
