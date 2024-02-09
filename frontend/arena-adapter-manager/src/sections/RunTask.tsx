import { Button } from "@chakra-ui/react";
import { ReactNode, useState } from "react";
import { Section } from "../components/Section";
import { ApiBase, MrApiTask, runTask } from "../core/api";

interface Props {
  base: ApiBase;
  task: MrApiTask;
  children: ReactNode;
}

export function RunTask(props: Props) {
  const [loading, setLoading] = useState(false);

  const executeTask = async () => {
    setLoading(true);
    await runTask(props.base, props.task);
    setLoading(false);
  };

  return (
    <Section headerText={props.task} loadingText={"Laster"} isLoading={loading}>
      <div>{props.children}</div>
      <Button disabled={loading} onClick={() => executeTask()}>
        Run task 💥
      </Button>
    </Section>
  );
}
