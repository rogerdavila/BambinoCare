package com.bambinocare.controller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.bambinocare.constant.ViewConstants;
import com.bambinocare.model.entity.BambinoEntity;
import com.bambinocare.model.entity.BookingEntity;
import com.bambinocare.model.entity.BookingStatusEntity;
import com.bambinocare.model.entity.BookingTypeEntity;
import com.bambinocare.model.entity.EmergencyContactEntity;
import com.bambinocare.model.entity.EventTypeEntity;
import com.bambinocare.model.entity.NannyEntity;
import com.bambinocare.model.entity.UserEntity;
import com.bambinocare.model.service.BambinoService;
import com.bambinocare.model.service.BookingService;
import com.bambinocare.model.service.BookingStatusService;
import com.bambinocare.model.service.BookingTypeService;
import com.bambinocare.model.service.EmailService;
import com.bambinocare.model.service.EventTypeService;
import com.bambinocare.model.service.NannyService;
import com.bambinocare.model.service.UserService;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	@Qualifier("userService")
	private UserService userService;

	@Autowired
	@Qualifier("bookingService")
	private BookingService bookingService;

	@Autowired
	@Qualifier("emailService")
	private EmailService emailService;

	@Autowired
	@Qualifier("nannyService")
	private NannyService nannyService;

	@Autowired
	@Qualifier("bookingTypeService")
	private BookingTypeService bookingTypeService;

	@Autowired
	@Qualifier("eventTypeService")
	private EventTypeService eventTypeService;

	@Autowired
	@Qualifier("bookingStatusService")
	private BookingStatusService bookingStatusService;

	@Autowired
	@Qualifier("bambinoService")
	private BambinoService bambinoService;

	@GetMapping("/showbookings")
	public ModelAndView showBookings(@RequestParam(required = false) String error,
			@RequestParam(required = false) String result) {

		ModelAndView mav = new ModelAndView(ViewConstants.ADMIN_VIEW);

		User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		UserEntity userLogged = userService.findByEmail(user.getUsername());

		mav.addObject("usernameLogged", userLogged.getFirstname());
		mav.addObject("bookings", bookingService.findAllBookings());
		mav.addObject("error", error);
		mav.addObject("result", result);

		return mav;
	}

	@PostMapping("/showbookingdetail")
	public String showBookingDetail(@RequestParam(name = "bookingid") Integer bookingId, Model model) {

		String error = "";
		String result = "";

		User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		UserEntity userEntity = userService.findByEmail(user.getUsername());

		BookingEntity booking = bookingService.findByBookingId(bookingId);

		if (booking != null) {

			if (booking.getBambino() != null) {
				List<String> bambinoIds = new ArrayList<>();
				for (BambinoEntity bambino : booking.getBambino()) {
					bambinoIds.add(bambino.getBambinoId().toString());
				}
				booking.setBambinoId(bambinoIds);
			}

			List<BookingTypeEntity> bookingTypes = bookingTypeService.findAllBookingTypes();
			List<EventTypeEntity> eventTypes = eventTypeService.findAllEventTypes();
			List<BambinoEntity> bambinos = bambinoService.findByClientUser(booking.getClient().getUser());

			model.addAttribute("allbambinos", bambinos);
			model.addAttribute("usernameLogged", userEntity.getFirstname());

			model.addAttribute("booking", booking);
			model.addAttribute("bookingTypes", bookingTypes);
			model.addAttribute("eventTypes", eventTypes);

			model.addAttribute("result", result);
			model.addAttribute("error", error);

			return ViewConstants.BOOKING_DETAIL_ADMIN_SHOW;
		} else {
			error = "No se encontró la reservación solicitada o no tiene permisos para verla";
		}

		return "redirect:/admin/showbookings?error=" + error + "&result=" + result;

	}

	@GetMapping("/editbookingform")
	public String showEditBooking(@RequestParam(required = false) String result,
			@RequestParam(required = false) String error, @RequestParam(required = true) Integer bookingId,
			Model model) {

		User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		UserEntity userEntity = userService.findByEmail(user.getUsername());

		BookingEntity booking = bookingService.findByBookingIdAndBookingStatusBookingStatusDescNotIn(bookingId,
				"Cancelada");

		if (booking == null) {
			error = "La reservación solicitada no existe o no tienes permisos para visualizarla o ya se encuentra cancelada";
			return "redirect:/admin/showbookings?error=" + error;
		}

		if (booking.getBambino() != null) {
			List<String> bambinoIds = new ArrayList<>();
			for (BambinoEntity bambino : booking.getBambino()) {
				bambinoIds.add(bambino.getBambinoId().toString());
			}
			booking.setBambinoId(bambinoIds);
		}

		List<BookingTypeEntity> bookingTypes = bookingTypeService.findAllBookingTypes();
		List<EventTypeEntity> eventTypes = eventTypeService.findAllEventTypes();
		List<BambinoEntity> bambinos = bambinoService.findByClientUser(booking.getClient().getUser());

		model.addAttribute("allbambinos", bambinos);
		model.addAttribute("usernameLogged", userEntity.getFirstname());

		model.addAttribute("booking", booking);
		model.addAttribute("bookingTypes", bookingTypes);
		model.addAttribute("eventTypes", eventTypes);

		model.addAttribute("result", result);
		model.addAttribute("error", error);

		return ViewConstants.BOOKING_ADMIN_EDIT;
	}

	@PostMapping("/editbooking")
	public ModelAndView editBooking(@ModelAttribute(name = "booking") BookingEntity booking,
			BindingResult bindingResult, Model model) {

		ModelAndView mav = new ModelAndView();

		String error = "";
		String result = "";

		if (booking.getDuration() == null || booking.getDuration() == 0) {
			error = "Favor de verificar el campo Duración";
			mav = new ModelAndView("redirect:/admin/showbookings");
			mav.addObject("error", error);
			return mav;
		}

		if (booking.getDate() == null) {
			error = "Favor de verificar el campo Fecha";
			mav = new ModelAndView("redirect:/admin/showbookings");
			mav.addObject("error", error);
			return mav;
		} else if (getDate(booking.getDate(), 1).before(getDate(Calendar.getInstance().getTime(), 0))) {
			error = "La reservación debe realizarse al menos 1 día antes de la fecha solictada";
			mav = new ModelAndView("redirect:/admin/showbookings");
			mav.addObject("error", error);
			return mav;
		}

		if (booking.getHour() == null || booking.getHour().equals("")) {
			error = "Favor de verificar el campo Hora";
			mav = new ModelAndView("redirect:/admin/showbookings");
			mav.addObject("error", error);
			return mav;
		}

		if (booking.getBambinoId().size() <= 0) {
			error = "Favor de elegir al menos un bambino";
			mav = new ModelAndView("redirect:/users/showbookings");
			mav.addObject("error", error);
			return mav;
		}

		booking.setBambino(
				bambinoService.findBambinosByBambinoIdAndUser(booking.getBambinoId(), booking.getClient().getUser()));

		if (booking.getBambino().isEmpty()) {
			error = "Ocurrió un error al intentar agregar a los bambinos";
			mav = new ModelAndView("redirect:/users/showbookings");
			mav.addObject("error", error);
			return mav;
		}

		BookingEntity oldBooking = bookingService.findByBookingId(booking.getBookingId());

		oldBooking.setDuration(booking.getDuration());
		oldBooking.setDate(getDate(booking.getDate(), 1));
		oldBooking.setHour(booking.getHour());
		oldBooking.setCost(booking.getDuration() * 200);
		oldBooking.setBambino(booking.getBambino());

		if (bookingService.createBooking(oldBooking) != null) {
			emailService.sendSimpleMessage(oldBooking.getClient().getUser().getEmail(), "rogerdavila.stech@gmail.com",
					"Reservación Modificada",
					"Su reservación del día " + oldBooking.getDate()
							+ "ha sido modificada. Puedes revisar el detalle en"
							+ " la siguiente liga: \n\r \n\r www.bambinocare.com");
			result = "La reservación fue modificada con éxito!";
		} else {
			result = "Ocurrió un error al intentar editar la reservación, vuelva a intentarlo";
		}

		mav = new ModelAndView("redirect:/admin/showbookings");
		mav.addObject("error", error);
		mav.addObject("result", result);
		return mav;
	}

	@PostMapping("/cancelbooking")
	public String cancelBooking(@RequestParam(name = "bookingid") Integer bookingId, Model model) {

		String error = "";
		String result = "";

		BookingEntity booking = bookingService.findByBookingIdAndBookingStatusBookingStatusDescNotIn(bookingId,
				"Cancelada");

		if (booking != null) {
			BookingStatusEntity bookingStatus = bookingStatusService.findByBookingStatusDesc("Cancelada");

			if (bookingStatus != null) {
				booking.setBookingStatus(bookingStatus);
				bookingService.createBooking(booking);
				result = "La cita ha sido cancelada";

				emailService.sendSimpleMessage(booking.getClient().getUser().getEmail(), "rogerdavila.stech@gmail.com",
						"Reservación Cancelada",
						"Su reservación del día del día " + booking.getDate()
								+ "  ha sido cancelada. Puedes revisar el detalle en"
								+ " la siguiente liga: \n\r \n\r www.bambinocare.com");

			} else {
				error = "No se permiten cancelaciones de reservación";
			}
		} else {
			error = "No se puede cancelar la reservación solicitada";
		}

		return "redirect:/admin/showbookings?error=" + error + "&result=" + result;
	}

	@GetMapping("/cancel")
	public String cancel() {
		return "redirect:/admin/showbookings";
	}

	@PostMapping("/approvebooking")
	public String approveBooking(@RequestParam(name = "bookingid", required = false) Integer bookingId,
			@ModelAttribute(name = "nanny") NannyEntity nanny, BindingResult bindingResult, Model model) {

		String error = "";
		String result = "";

		BookingEntity booking = bookingService.findByBookingIdAndBookingStatusBookingStatusDescNotIn(bookingId,
				"Cancelada", "Agendada", "Rechazada");

		if (booking != null) {

			if (nanny.getNannyId() != null && booking.getNanny() == null) {
				booking.setNanny(nanny);
			} else if (booking.getNanny() == null) {
				NannyEntity nannyToAssign = new NannyEntity();
				List<NannyEntity> nannies = nannyService.findAllNannies();
				model.addAttribute("nanny", nannyToAssign);
				model.addAttribute("nannies", nannies);
				model.addAttribute("bookingId", booking.getBookingId());
				return ViewConstants.NANNY_ASSIGN;
			}

			bookingService.createBooking(booking);
			BookingStatusEntity bookingStatus = bookingStatusService.findByBookingStatusDesc("Agendada");

			if (bookingStatus != null) {
				booking.setBookingStatus(bookingStatus);
				bookingService.createBooking(booking);
				result = "La cita ha sido agendada";

				emailService.sendSimpleMessage(booking.getClient().getUser().getEmail(), "rogerdavila.stech@gmail.com",
						"Reservación Agendada",
						"Su reservación del día del día " + booking.getDate()
								+ "  ha sido agendada. Puedes revisar el detalle en"
								+ " la siguiente liga: \n\r \n\r www.bambinocare.com");

			} else {
				error = "No se permite agendar esta reservación";
			}
		} else {
			error = "No se puede agendar la reservación solicitada";
		}

		return "redirect:/admin/showbookings?error=" + error + "&result=" + result;
	}

	@PostMapping("/rejectbooking")
	public String rejectBooking(@RequestParam(name = "bookingid") Integer bookingId, Model model) {

		String error = "";
		String result = "";

		BookingEntity booking = bookingService.findByBookingIdAndBookingStatusBookingStatusDescNotIn(bookingId,
				"Cancelada", "Agendada", "Rechazada");

		if (booking != null) {
			BookingStatusEntity bookingStatus = bookingStatusService.findByBookingStatusDesc("Rechazada");

			if (bookingStatus != null) {
				booking.setBookingStatus(bookingStatus);
				bookingService.createBooking(booking);
				result = "La cita ha sido rechazada";

				emailService.sendSimpleMessage(booking.getClient().getUser().getEmail(), "rogerdavila.stech@gmail.com",
						"Reservación Rechazada",
						"Su reservación del día " + booking.getDate()
								+ "  ha sido rechazada. Puedes revisar el detalle en"
								+ " la siguiente liga: \n\r \n\r www.bambinocare.com");

			} else {
				error = "No se permite rechazar la reservación solicitada";
			}
		} else {
			error = "No se puede rechazar la reservación solicitada";
		}

		return "redirect:/admin/showbookings?error=" + error + "&result=" + result;
	}
	
	
	
	@GetMapping("/createnannyform")
	public String createNannyForm(@RequestParam(required = false) String result,
			@RequestParam(required = false) String error, Model model) {
		EmergencyContactEntity nanny = new EmergencyContactEntity();

		User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		UserEntity userEntity = userService.findByEmail(user.getUsername());
		model.addAttribute("usernameLogged", userEntity.getFirstname());

		model.addAttribute("nanny", nanny);
		model.addAttribute("result", result);
		model.addAttribute("error", error);

		return ViewConstants.CREATE_NANNY;
	}

	public static Date getDate(Date date, int days) {

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_YEAR, days);

		return calendar.getTime();
	}

}
