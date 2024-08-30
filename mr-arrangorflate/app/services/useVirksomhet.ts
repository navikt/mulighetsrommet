import { useSearchParams } from "@remix-run/react";

const useVirksomhet = () => {
    const [searchParams] = useSearchParams();

    return searchParams.get("virksomhet");
};

export default useVirksomhet;
