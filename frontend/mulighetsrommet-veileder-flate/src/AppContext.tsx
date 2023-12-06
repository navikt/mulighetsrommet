import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { PrimitiveAtom, Provider, SetStateAction, useAtom } from "jotai";
import { useHydrateAtoms } from "jotai/utils";
import { ReactNode, useEffect } from "react";
import { appContext } from "./core/atoms/atoms";
import { AppContextData } from "./hooks/useAppContext";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      throwOnError: true,
      retry: 3,
    },
  },
});

export interface AppContextProps {
  contextData: Partial<AppContextData>;
  updateContextDataRef?: (updateContextData: (key: string, value: string) => void) => void;
  children: ReactNode;
}

function HydrateAtoms<T>({
  initialValues,
  children,
}: {
  initialValues: [PrimitiveAtom<T>, SetStateAction<T>][];
  children: ReactNode;
}) {
  // initialising on state with prop on render here
  useHydrateAtoms(initialValues);
  return children;
}

export function AppContext(props: AppContextProps) {
  const [contextData, setContextData] = useAtom(appContext);

  useEffect(() => {
    if (props.contextData) {
      props?.updateContextDataRef?.((key: string, value: string) => {
        setContextData({ ...contextData, [key]: value });
      });
      // console.log("Setter data fra props");
      // setContextData({ ...contextData, ...props.contextData });
    }
  }, [props.contextData.enhet, props.contextData.fnr]);

  return (
    <QueryClientProvider client={queryClient}>
      <Provider>
        <HydrateAtoms initialValues={[[appContext, props.contextData]]}>
          {props.children}
        </HydrateAtoms>
      </Provider>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}
