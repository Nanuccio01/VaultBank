import { http } from "./http";

export type RegisterReq = {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    password: string;
};

export type LoginReq = {
    email: string;
    password: string;
};

export type TokenResp = {
    accessToken: string;
    tokenType: string;
    expiresInSeconds: number;
    scope: string;
};

export async function register(req: RegisterReq): Promise<void> {
    await http.post("/api/auth/register", req);
}

export async function login(req: LoginReq): Promise<TokenResp> {
    const { data } = await http.post<TokenResp>("/api/auth/login", req);
    return data;
}