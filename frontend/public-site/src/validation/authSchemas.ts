import { z } from 'zod';

// TODO:
// - type requests un AuthService
// - unify with API (add passwordConfirm)
type RegistrationRequest = {
  name: string;
  email: string;
  password: string;
  passwordConfirm: string;
};

export const loginSchema = z.object({
  email: z.string().email('The email is invalid or missing'),
  password: z.string().min(8, 'Password must be at least 8 characters long'),
});

export const registerSchema = z
  .object({
    name: z.string().min(1, 'Please enter your name'),
    email: z.string().email('The email is invalid or missing'),
    password: z.string().min(8, 'Password must be at least 8 characters long'),
    passwordConfirm: z.string(),
  })
  .refine((data: RegistrationRequest) => data.password === data.passwordConfirm, {
    message: 'Passwords do not match',
    path: ['passwordConfirm'],
  });
