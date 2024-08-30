import { logger } from "server/logger";

export const virksomhetErFeatureTogglet = (virksomhet: string) => {
    if (process.env.NODE_ENV === "development") {
        return true;
    }

    const verdi = process.env.PRESENTERTE_KANDIDATER_TOGGLE;

    if (verdi === undefined) {
        logger.error(
            `Kunne ikke hente togglede virksomheter fordi miljøvariabelen "PRESENTERTE_KANDIDATER_TOGGLE" ikke finnes`
        );
        return false;
    } else {
        return verdi.split(",").includes(virksomhet);
    }
};
