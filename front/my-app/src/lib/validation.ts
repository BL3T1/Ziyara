import { z } from 'zod'

export const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Invalid email address'),
  password: z.string().min(1, 'Password is required'),
})

export const signUpSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Invalid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

export const listingSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters').max(120, 'Name too long'),
  description: z.string().min(10, 'Description must be at least 10 characters').max(2000, 'Description too long'),
  price: z.number({ error: 'Price must be a number' }).positive('Price must be positive'),
  currency: z.string().min(1, 'Currency is required'),
  city: z.string().optional(),
  country: z.string().optional(),
  category: z.enum(['hotels', 'resorts', 'restaurants', 'trips', 'taxis']),
})

// Portal listing form uses ServiceTypeDto values (uppercase) and basePrice as a parseable string
export const portalListingSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters').max(120, 'Name is too long'),
  description: z.string().max(2000, 'Description must be under 2000 characters').optional(),
  basePrice: z
    .string()
    .min(1, 'Price is required')
    .refine((v) => {
      const n = parseFloat(v.replace(/,/g, ''))
      return Number.isFinite(n) && n >= 0
    }, 'Price must be a valid non-negative number'),
  currency: z.string().min(1, 'Currency is required').max(10, 'Invalid currency'),
})

export const checkoutSchema = z.object({
  discountCode: z.string().optional(),
  specialRequests: z.string().max(500, 'Special requests must be under 500 characters').optional(),
})

export type LoginFormData = z.infer<typeof loginSchema>
export type SignUpFormData = z.infer<typeof signUpSchema>
export type ListingFormData = z.infer<typeof listingSchema>
export type PortalListingFormData = z.infer<typeof portalListingSchema>
export type CheckoutFormData = z.infer<typeof checkoutSchema>
