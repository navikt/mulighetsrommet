/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { request as __request } from '../core/request';

export class InternalService {

    /**
     * @returns string API is online and responding
     * @throws ApiError
     */
    public static getInternal(): CancelablePromise<'PONG'> {
        return __request({
            method: 'GET',
            path: `/internal/ping`,
        });
    }

}