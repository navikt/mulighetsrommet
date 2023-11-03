import { Button } from "@chakra-ui/react";
import { useState } from "react";
import { Section } from "../components/Section";
import { MrApiTask, runTask } from "../core/api";

interface Props {
  task: MrApiTask;
}

export function RunTask(props: Props) {
  const [loading, setLoading] = useState(false);

  const executeTask = async () => {
    setLoading(true);
    await runTask(props.task);
    setLoading(false);
  };

  return (
    <Section headerText={props.task} loadingText={"Laster"} isLoading={loading}>
      <Button disabled={loading} onClick={() => executeTask()}>
        Run task 💥
      </Button>
    </Section>
  );
}
