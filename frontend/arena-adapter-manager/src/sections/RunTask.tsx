import { Button } from "@chakra-ui/react";
import { ReactNode, useState } from "react";
import { Section } from "../components/Section";
import { ApiBase, MrApiTask, runTask } from "../core/api";
import validator from "@rjsf/validator-ajv8";
import { RJSFSchema } from "@rjsf/utils";
import Form from "@rjsf/chakra-ui";

interface Props {
  base: ApiBase;
  task: MrApiTask;
  input?: RJSFSchema;
  children?: ReactNode;
}

export function RunTask(props: Props) {
  const [loading, setLoading] = useState(false);

  const executeTask = (input?: object) => {
    setLoading(true);

    runTask(props.base, props.task, input).finally(() => {
      setLoading(false);
    });
  };

  return (
    <Section headerText={props.task} loadingText={"Laster"} isLoading={loading}>
      {props.children && <div>{props.children}</div>}

      {props.input ? (
        <Form
          schema={props.input}
          validator={validator}
          onSubmit={({ formData }) => {
            executeTask(formData);
          }}
        >
          <div>
            <Button type="submit" disabled={loading}>
              Run task 💥
            </Button>
          </div>
        </Form>
      ) : (
        <Button disabled={loading} onClick={() => executeTask()}>
          Run task 💥
        </Button>
      )}
    </Section>
  );
}
